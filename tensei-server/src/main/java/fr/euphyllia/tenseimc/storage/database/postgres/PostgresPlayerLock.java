package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.api.PlayerLock;
import fr.euphyllia.tenseimc.storage.database.postgres.util.SQLExecute;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PostgresPlayerLock implements PlayerLock {

    private static final Logger LOGGER = LogUtils.getLogger();

    PostgresPlayerLock() {
    }


    @Override
    public CompletableFuture<Boolean> tryAcquire(UUID playerUuid) {
        final String schema = PostgresManager.schema();
        final long ttl = PostgresManager.config().postgres().lock().ttlSeconds();

        final String sql = """
                INSERT INTO %s.player_lock (uuid, locked_by, locked_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (uuid) DO UPDATE SET
                    locked_by = EXCLUDED.locked_by,
                    locked_at = NOW()
                WHERE %s.player_lock.locked_by = EXCLUDED.locked_by
                   OR %s.player_lock.locked_at < NOW() - INTERVAL '%d seconds'
                """.formatted(schema, schema, schema, ttl);

        return CompletableFuture.supplyAsync(() -> {
            final int affected = SQLExecute.updateOnLogin(
                    "tryAcquire lock for " + playerUuid,
                    sql,
                    List.of(playerUuid, PostgresManager.serverId())
            );
            return affected > 0;
        }, PostgresManager.ioExecutor());
    }

    @Override
    public CompletableFuture<Boolean> refresh(UUID playerUuid) {
        final String sql = """
                UPDATE %s.player_lock
                SET locked_at = NOW()
                WHERE uuid = ? AND locked_by = ?
                """.formatted(PostgresManager.schema());

        return CompletableFuture.supplyAsync(() -> {
            final int affected = SQLExecute.update(
                    "refresh lock for " + playerUuid,
                    sql,
                    List.of(playerUuid, PostgresManager.serverId())
            );
            if (affected == 0) {
                LOGGER.warn("Lock refresh failed for {}: lock no longer held by this server", playerUuid);
                return false;
            }
            return true;
        }, PostgresManager.ioExecutor());
    }

    @Override
    public CompletableFuture<Void> release(UUID playerUuid) {
        final String sql = """
                DELETE FROM %s.player_lock
                WHERE uuid = ? AND locked_by = ?
                """.formatted(PostgresManager.schema());

        return CompletableFuture.runAsync(() -> {
            try {
                SQLExecute.update(
                        "release lock for " + playerUuid,
                        sql,
                        List.of(playerUuid, PostgresManager.serverId())
                );
            } catch (final Exception ex) {
                LOGGER.warn("Failed to release lock for {}", playerUuid, ex);
            }
        }, PostgresManager.ioExecutor());
    }
}
