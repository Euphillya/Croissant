package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.euphyllia.tenseimc.storage.config.StorageConfig;
import fr.euphyllia.tenseimc.storage.database.postgres.util.Identifiers;
import fr.euphyllia.tenseimc.storage.model.StorageHealth;
import fr.euphyllia.tenseimc.storage.resilience.StorageFailureHandler;
import fr.euphyllia.tenseimc.storage.resilience.StrictKickFailureHandler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PostgresManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean enabled = false;
    private static volatile StorageConfig config;
    private static volatile String serverId;
    private static volatile String schema;
    private static volatile MinecraftServer server;
    private static volatile HikariDataSource loginDataSource;
    private static volatile HikariDataSource saveDataSource;
    private static volatile ExecutorService ioExecutor;
    private static volatile PostgresPlayerLock playerLock;
    private static volatile StorageFailureHandler failureHandler;
    private static volatile PostgresHealthCheck healthCheck;
    private static volatile PostgresLockHeartbeat lockHeartbeat;
    private static final AtomicReference<StorageHealth> health = new AtomicReference<>(StorageHealth.HEALTHY);
    private static final AtomicInteger pendingSaves = new AtomicInteger(0);

    private PostgresManager() {
    }

    public static synchronized void init(
            final StorageConfig cfg,
            final MinecraftServer mcServer,
            final Path dataDir
    ) throws IOException, SQLException {
        if (enabled) {
            throw new IllegalStateException("PostgresManager already initialized");
        }
        if (cfg.backend() == StorageConfig.Backend.FILE) {
            LOGGER.info("Storage backend is FILE — Postgres manager not started, vanilla behavior preserved");
            return;
        }

        config = cfg;
        server = mcServer;
        serverId = resolveServerId(cfg, dataDir);
        schema = Identifiers.sanitize(cfg.postgres().schema());
        if (schema.isEmpty()) {
            schema = "public";
        }

        LOGGER.info("Initializing PostgresManager: serverId={}, schema={}", serverId, schema);

        // Phase 1 : bootstrap (création de la BDD si nécessaire)
        try {
            PostgresBootstrap.ensureDatabaseExists(cfg.postgres());
        } catch (final SQLException ex) {
            throw new SQLException("Failed to bootstrap PostgreSQL database", ex);
        }

        // Phase 2 : pools HikariCP
        loginDataSource = createDataSource("tenseimc-login", cfg.postgres().pool().loginPoolSize());
        saveDataSource = createDataSource("tenseimc-save", cfg.postgres().pool().savePoolSize());
        ioExecutor = Executors.newFixedThreadPool(
                Math.max(4, cfg.postgres().pool().savePoolSize()),
                namedThreadFactory("tenseimc-storage-io")
        );

        enabled = true;

        // Phase 3 : schéma SQL
        try {
            new PostgresSchemaInitializer(schema).init();
        } catch (final Exception ex) {
            shutdownInternal(false);
            enabled = false;
            throw new SQLException("Failed to initialize Postgres schema", ex);
        }

        // Phase 4 : services dépendants (lock, healthcheck, heartbeat, failure handler)
        try {
            playerLock = new PostgresPlayerLock();
            failureHandler = createFailureHandler(mcServer, cfg);
            healthCheck = new PostgresHealthCheck();
            healthCheck.start();
            lockHeartbeat = new PostgresLockHeartbeat();
            lockHeartbeat.start();
        } catch (final Exception ex) {
            shutdownInternal(false);
            enabled = false;
            throw new SQLException("Failed to initialize Postgres dependent services", ex);
        }

        LOGGER.info("PostgresManager initialized successfully");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static StorageConfig config() {
        ensureEnabled();
        return config;
    }

    public static String serverId() {
        ensureEnabled();
        return serverId;
    }

    public static String schema() {
        ensureEnabled();
        return schema;
    }

    public static MinecraftServer server() {
        ensureEnabled();
        return server;
    }

    public static DataSource loginDataSource() {
        ensureEnabled();
        return loginDataSource;
    }

    public static DataSource saveDataSource() {
        ensureEnabled();
        return saveDataSource;
    }

    public static ExecutorService ioExecutor() {
        ensureEnabled();
        return ioExecutor;
    }

    public static PostgresPlayerLock playerLock() {
        ensureEnabled();
        return playerLock;
    }

    public static StorageFailureHandler failureHandler() {
        ensureEnabled();
        return failureHandler;
    }

    public static PostgresLockHeartbeat lockHeartbeat() {
        ensureEnabled();
        return lockHeartbeat;
    }

    public static StorageHealth health() {
        return health.get();
    }

    static void setHealth(final StorageHealth newHealth) {
        final StorageHealth previous = health.getAndSet(newHealth);
        if (previous != newHealth) {
            LOGGER.info("Storage health transition: {} -> {}", previous, newHealth);
        }
    }

    public static int pendingSavesCount() {
        return pendingSaves.get();
    }


    public static boolean submitSave(final Runnable task) {
        final ExecutorService executor = ioExecutor;
        if (executor == null || executor.isShutdown()) {
            return false;
        }
        pendingSaves.incrementAndGet();
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    pendingSaves.decrementAndGet();
                }
            });
            return true;
        } catch (final RejectedExecutionException ex) {
            pendingSaves.decrementAndGet();
            return false;
        }
    }

    public static synchronized void shutdown() {
        if (!enabled) return;
        LOGGER.info("Shutting down PostgresManager");
        shutdownInternal(true);
        enabled = false;
    }

    private static void shutdownInternal(final boolean drainPendingSaves) {
        if (lockHeartbeat != null) {
            lockHeartbeat.close();
            lockHeartbeat = null;
        }
        if (healthCheck != null) {
            healthCheck.close();
            healthCheck = null;
        }
        if (ioExecutor != null) {
            ioExecutor.shutdown();
            if (drainPendingSaves) {
                drainWithProgress();
            }
            ioExecutor = null;
        }
        if (loginDataSource != null) {
            loginDataSource.close();
            loginDataSource = null;
        }
        if (saveDataSource != null) {
            saveDataSource.close();
            saveDataSource = null;
        }
    }


    private static void drainWithProgress() {
        final int totalAtStart = pendingSaves.get();
        final long timeoutMs = config != null
                ? config.resilience().shutdown().flushTimeoutMs()
                : 30_000L;
        final long startNanos = System.nanoTime();
        final long deadlineNanos = startNanos + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

        if (totalAtStart == 0) {
            LOGGER.info("No pending saves, shutdown ready");
            return;
        }

        LOGGER.info("Draining {} pending saves (timeout: {}ms)", totalAtStart, timeoutMs);

        final long pollChunkMs = 500;

        int nextThreshold = (int) Math.ceil(totalAtStart * 0.9);
        final int step = Math.max(1, totalAtStart / 10);

        long lastProgressNanos = startNanos;
        int lastPending = totalAtStart;

        try {
            while (true) {
                final long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    final int pending = pendingSaves.get();
                    LOGGER.error("Drain timeout ({}ms) reached - forcing shutdown, {} saves lost",
                            timeoutMs, pending);
                    ioExecutor.shutdownNow();
                    return;
                }

                final long waitMs = Math.min(pollChunkMs, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
                final boolean terminated = ioExecutor.awaitTermination(waitMs, TimeUnit.MILLISECONDS);

                final int pending = pendingSaves.get();
                final int done = totalAtStart - pending;

                if (terminated || pending == 0) {
                    final long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                    LOGGER.info("All {} saves flushed in {}ms", totalAtStart, elapsed);
                    return;
                }

                if (pending <= nextThreshold) {
                    final long elapsed = (System.nanoTime() - startNanos) / 1_000_000;
                    final int percent = (done * 100) / totalAtStart;
                    LOGGER.info("Drain progress: {}/{} ({}%) - {}ms elapsed",
                            done, totalAtStart, percent, elapsed);
                    nextThreshold -= step;
                }

                if (pending != lastPending) {
                    lastPending = pending;
                    lastProgressNanos = System.nanoTime();
                } else if (System.nanoTime() - lastProgressNanos > TimeUnit.SECONDS.toNanos(30)) {
                    LOGGER.warn("Drain stalled at {}/{} for 30s - Postgres may be unresponsive",
                            done, totalAtStart);
                    lastProgressNanos = System.nanoTime();
                }
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            final int pending = pendingSaves.get();
            LOGGER.warn("Interrupted during drain - forcing shutdown, {} saves lost", pending);
            ioExecutor.shutdownNow();
        }
    }

    private static void ensureEnabled() {
        if (!enabled) {
            throw new IllegalStateException("PostgresManager is not enabled (backend=FILE or not initialized)");
        }
    }

    private static HikariDataSource createDataSource(final String poolName, final int maxPoolSize) {
        final StorageConfig.Postgres pg = config.postgres();
        final StorageConfig.Postgres.Pool poolConfig = pg.pool();

        final HikariConfig hc = new HikariConfig();
        hc.setPoolName(poolName);
        hc.setDriverClassName("org.postgresql.Driver");
        hc.setJdbcUrl(pg.jdbcUrl());
        hc.setUsername(pg.username());
        hc.setPassword(pg.password());
        hc.setMaximumPoolSize(maxPoolSize);
        hc.setMinimumIdle(Math.min(2, maxPoolSize));
        hc.setConnectionTimeout(poolConfig.connectionTimeoutMs());
        hc.setIdleTimeout(poolConfig.idleTimeoutMs());
        hc.setMaxLifetime(poolConfig.maxLifetimeMs());
        hc.setKeepaliveTime(poolConfig.keepaliveTimeMs());
        hc.addDataSourceProperty("ApplicationName", "tenseimc-" + serverId);
        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.addDataSourceProperty("reWriteBatchedInserts", "true");
        return new HikariDataSource(hc);
    }

    private static StorageFailureHandler createFailureHandler(
            final MinecraftServer srv, final StorageConfig cfg
    ) {
        return switch (cfg.resilience().autosave().mode()) {
            case STRICT_KICK -> new StrictKickFailureHandler(srv, cfg.resilience().autosave().kickMessage());
            case RETRY_THEN_KICK -> {
                LOGGER.warn("Mode RETRY_THEN_KICK not yet implemented, falling back to STRICT_KICK");
                yield new StrictKickFailureHandler(srv, cfg.resilience().autosave().kickMessage());
            }
            case BUFFERED -> {
                LOGGER.warn("Mode BUFFERED not yet implemented, falling back to STRICT_KICK");
                yield new StrictKickFailureHandler(srv, cfg.resilience().autosave().kickMessage());
            }
        };
    }

    private static String resolveServerId(final StorageConfig cfg, final Path dataDir) throws IOException {
        if (!cfg.serverId().isBlank()) {
            return cfg.serverId();
        }
        final Path idFile = dataDir.resolve("tenseimc-server-id");
        if (Files.exists(idFile)) {
            return Files.readString(idFile).trim();
        }
        Files.createDirectories(dataDir);
        final String generated = UUID.randomUUID().toString();
        Files.writeString(idFile, generated);
        LOGGER.info("Generated new server ID: {} (stored at {})", generated, idFile);
        return generated;
    }

    private static ThreadFactory namedThreadFactory(final String prefix) {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(final @NotNull Runnable r) {
                final Thread t = new Thread(r, prefix + "-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
    }

}
