package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.config.StorageConfig;
import fr.euphyllia.tenseimc.storage.model.StorageHealth;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PostgresHealthCheck implements AutoCloseable {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int QUERY_TIMEOUT_SECONDS = 2;

    private final ScheduledExecutorService scheduler;
    private final long checkIntervalMs;
    private final int failureThreshold;
    private final int recoveryThreshold;

    private int consecutiveFailures = 0;
    private int consecutiveSuccesses = 0;

    PostgresHealthCheck() {
        final StorageConfig.Resilience.Health cfg = PostgresManager.config().resilience().health();
        this.checkIntervalMs = cfg.checkIntervalMs();
        this.failureThreshold = Math.max(0, cfg.failureThreshold());
        this.recoveryThreshold = Math.max(1, cfg.recoveryThreshold());

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "tenseimc-storage-healthcheck");
            t.setDaemon(true);
            return t;
        });
    }

    void start() {
        scheduler.scheduleWithFixedDelay(this::check, 0, checkIntervalMs, TimeUnit.MILLISECONDS);
        LOGGER.info("Healthcheck started: interval={}ms, failureThreshold={}, recoveryThreshold={}",
                checkIntervalMs, failureThreshold, recoveryThreshold);
    }

    private synchronized void check() {
        try (Connection conn = PostgresManager.loginDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            stmt.execute("SELECT 1");
            onCheckSuccess();
        } catch (final SQLException ex) {
            onCheckFailure("SQL error: " + ex.getMessage());
        } catch (final Exception ex) {
            onCheckFailure("Unexpected error: " + ex.getMessage());
        }
    }

    private void onCheckSuccess() {
        consecutiveFailures = 0;
        consecutiveSuccesses++;

        final StorageHealth currentHealth = PostgresManager.health();

        if (currentHealth == StorageHealth.DOWN && consecutiveSuccesses >= recoveryThreshold) {
            LOGGER.info("Storage recovered after {} successful checks — accepting connections again",
                    consecutiveSuccesses);
            PostgresManager.setHealth(StorageHealth.HEALTHY);
            PostgresManager.onStorageRecovered();
        } else if (currentHealth != StorageHealth.HEALTHY && consecutiveSuccesses >= recoveryThreshold) {
            PostgresManager.setHealth(StorageHealth.HEALTHY);
        }
    }

    private void onCheckFailure(final String reason) {
        consecutiveSuccesses = 0;
        consecutiveFailures++;

        LOGGER.warn("Healthcheck failed ({}/{}): {}",
                consecutiveFailures, failureThreshold + 1, reason);

        if (consecutiveFailures > failureThreshold) {
            final StorageHealth currentHealth = PostgresManager.health();
            if (currentHealth != StorageHealth.DOWN) {
                LOGGER.error("Storage marked DOWN after {} consecutive failures — kicking all players",
                        consecutiveFailures);
                PostgresManager.setHealth(StorageHealth.DOWN);
                PostgresManager.onStorageDown();
            }
        } else {
            // Sous le seuil : on est en transition, statut DEGRADED
            if (PostgresManager.health() == StorageHealth.HEALTHY) {
                PostgresManager.setHealth(StorageHealth.DEGRADED);
            }
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
