package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
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
    private static final long CHECK_INTERVAL_SECONDS = 5;
    private static final int QUERY_TIMEOUT_SECONDS = 2;

    private final ScheduledExecutorService scheduler;

    PostgresHealthCheck() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "tenseimc-storage-healthcheck");
            t.setDaemon(true);
            return t;
        });
    }

    void start() {
        scheduler.scheduleWithFixedDelay(this::check, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void check() {
        try (Connection conn = PostgresManager.loginDataSource().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            stmt.execute("SELECT 1");
            PostgresManager.setHealth(StorageHealth.HEALTHY);
        } catch (final SQLException ex) {
            LOGGER.warn("Storage healthcheck failed: {}", ex.getMessage());
            PostgresManager.setHealth(StorageHealth.DOWN);
        } catch (final Exception ex) {
            LOGGER.warn("Unexpected error in storage healthcheck", ex);
            PostgresManager.setHealth(StorageHealth.DEGRADED);
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
