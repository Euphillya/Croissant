package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PostgresLockHeartbeat implements AutoCloseable {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ScheduledExecutorService scheduler;
    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();

    PostgresLockHeartbeat() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "tenseimc-lock-heartbeat");
            t.setDaemon(true);
            return t;
        });
    }

    void start() {
        final long interval = PostgresManager.config().postgres().lock().heartbeatIntervalMs();
        scheduler.scheduleWithFixedDelay(this::refreshAll, interval, interval, TimeUnit.MILLISECONDS);
        LOGGER.info("Lock heartbeat started (interval = {} ms)", interval);
    }

    public void track(final UUID playerUuid) {
        trackedPlayers.add(playerUuid);
    }

    public void untrack(final UUID playerUuid) {
        trackedPlayers.remove(playerUuid);
    }

    private void refreshAll() {
        for (final UUID uuid : trackedPlayers) {
            PostgresManager.playerLock().refresh(uuid).whenComplete((success, throwable) -> {
                if (throwable != null) {
                    LOGGER.warn("Lock heartbeat error for {}", uuid, throwable);
                } else if (Boolean.FALSE.equals(success)) {
                    LOGGER.warn("Lock heartbeat for {} returned false: lock may have been stolen by another server", uuid);
                }
            });
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        trackedPlayers.clear();
    }
}
