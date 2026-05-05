package fr.euphyllia.tenseimc.storage.database.postgres;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fr.euphyllia.tenseimc.storage.database.postgres.util.SQLExecute;
import fr.euphyllia.tenseimc.storage.exception.PermanentStorageException;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PostgresStatsAccess {

    private PostgresStatsAccess() {
    }

    private static String upsertSql() {
        return """
                INSERT INTO %s.player_stats (uuid, data, updated_at)
                VALUES (?, ?::jsonb, NOW())
                ON CONFLICT (uuid) DO UPDATE SET
                    data = EXCLUDED.data,
                    updated_at = NOW()
                """.formatted(PostgresManager.schema());
    }

    private static String selectSql() {
        return "SELECT data FROM %s.player_stats WHERE uuid = ?".formatted(PostgresManager.schema());
    }

    public static void save(final UUID uuid, final JsonElement data) {
        final PGobject jsonObj = new PGobject();
        jsonObj.setType("jsonb");
        try {
            jsonObj.setValue(data.toString());
        } catch (final SQLException ex) {
            throw new PermanentStorageException("Failed to wrap JSON for stats " + uuid, ex);
        }

        SQLExecute.update(
                "save stats for " + uuid,
                upsertSql(),
                List.of(uuid, jsonObj)
        );
    }

    public static Optional<JsonElement> load(final UUID uuid) {
        return Optional.ofNullable(SQLExecute.queryMapOnLogin(
                "load stats for " + uuid,
                selectSql(),
                List.of(uuid),
                rs -> {
                    try {
                        if (!rs.next()) return null;
                        final String raw = rs.getString("data");
                        if (raw == null || raw.isBlank()) return null;
                        return JsonParser.parseString(raw);
                    } catch (final SQLException ex) {
                        throw new PermanentStorageException("Failed to read stats for " + uuid, ex);
                    } catch (final Exception ex) {
                        throw new PermanentStorageException("Corrupted stats JSON for " + uuid, ex);
                    }
                }
        ));
    }
}
