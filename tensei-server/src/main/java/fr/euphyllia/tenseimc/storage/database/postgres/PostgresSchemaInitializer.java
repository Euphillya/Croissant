package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.database.postgres.util.Identifiers;
import fr.euphyllia.tenseimc.storage.database.postgres.util.SQLExecute;
import org.slf4j.Logger;

public class PostgresSchemaInitializer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CREATE_PLAYER_DATA_TABLE = """
            CREATE TABLE IF NOT EXISTS %s.player_data (
                uuid          UUID PRIMARY KEY,
                name          VARCHAR(16) NOT NULL,
                data          BYTEA NOT NULL,
                data_version  INTEGER NOT NULL,
                updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );
            """;

    private static final String CREATE_PLAYER_DATA_NAME_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_player_data_name
            ON %s.player_data (LOWER(name));
            """;

    private static final String CREATE_PLAYER_DATA_UPDATED_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_player_data_updated
            ON %s.player_data (updated_at);
            """;

    private static final String CREATE_PLAYER_LOCK_TABLE = """
            CREATE TABLE IF NOT EXISTS %s.player_lock (
                uuid       UUID PRIMARY KEY,
                locked_by  VARCHAR(64) NOT NULL,
                locked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );
            """;

    private static final String CREATE_PLAYER_LOCK_LOCKED_BY_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_player_lock_locked_by
            ON %s.player_lock (locked_by);
            """;

    private static final String CREATE_PLAYER_ADVANCEMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS %s.player_advancements (
                uuid        UUID PRIMARY KEY,
                data        JSONB NOT NULL,
                updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );
            """;

    private static final String CREATE_PLAYER_STATS_TABLE = """
            CREATE TABLE IF NOT EXISTS %s.player_stats (
                uuid        UUID PRIMARY KEY,
                data        JSONB NOT NULL,
                updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
            );
            """;

    private final String schema;

    PostgresSchemaInitializer(final String schema) {
        this.schema = Identifiers.sanitize(schema);
    }

    void init() {
        createSchemaIfNeeded();
        createTables();
        createIndexes();
        LOGGER.info("Postgres schema '{}' initialized", schema);
    }

    private void createSchemaIfNeeded() {
        if ("public".equalsIgnoreCase(schema)) return;
        SQLExecute.update(
                "create schema",
                "CREATE SCHEMA IF NOT EXISTS " + schema,
                null
        );
    }

    private void createTables() {
        exec("create player_data table", CREATE_PLAYER_DATA_TABLE.formatted(schema));
        exec("create player_lock table", CREATE_PLAYER_LOCK_TABLE.formatted(schema));
        exec("create player_advancements table", CREATE_PLAYER_ADVANCEMENTS_TABLE.formatted(schema));
        exec("create player_stats table", CREATE_PLAYER_STATS_TABLE.formatted(schema));
    }

    private void createIndexes() {
        exec("create idx_player_data_name", CREATE_PLAYER_DATA_NAME_INDEX.formatted(schema));
        exec("create idx_player_data_updated", CREATE_PLAYER_DATA_UPDATED_INDEX.formatted(schema));
        exec("create idx_player_lock_locked_by", CREATE_PLAYER_LOCK_LOCKED_BY_INDEX.formatted(schema));
    }

    private void exec(final String operation, final String sql) {
        SQLExecute.update(operation, sql, null);
    }

}
