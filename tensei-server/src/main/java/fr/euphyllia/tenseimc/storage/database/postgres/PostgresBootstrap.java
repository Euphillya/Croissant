package fr.euphyllia.tenseimc.storage.database.postgres;

import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.config.StorageConfig;
import org.slf4j.Logger;

import java.sql.*;
import java.util.Properties;

public class PostgresBootstrap {


    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BOOTSTRAP_CONNECT_TIMEOUT_SECONDS = 5;
    private static final int BOOTSTRAP_QUERY_TIMEOUT_SECONDS = 5;

    private PostgresBootstrap() {
    }


    static void ensureDatabaseExists(final StorageConfig.Postgres pgConfig) throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (final ClassNotFoundException ex) {
            throw new SQLException("PostgreSQL JDBC driver not found in classpath", ex);
        }

        final String bootstrapUrl = pgConfig.bootstrapJdbcUrl();
        final String dbName = pgConfig.database();

        final Properties props = new Properties();
        props.setProperty("user", pgConfig.username());
        props.setProperty("password", pgConfig.password());
        props.setProperty("connectTimeout", String.valueOf(BOOTSTRAP_CONNECT_TIMEOUT_SECONDS));
        props.setProperty("loginTimeout", String.valueOf(BOOTSTRAP_CONNECT_TIMEOUT_SECONDS));
        props.setProperty("socketTimeout", String.valueOf(BOOTSTRAP_QUERY_TIMEOUT_SECONDS));
        props.setProperty("ApplicationName", "tenseimc-bootstrap");

        try (Connection conn = DriverManager.getConnection(bootstrapUrl, pgConfig.username(), pgConfig.password())) {
            if (databaseExists(conn, dbName)) {
                LOGGER.info("PostgreSQL database '{}' already exists", dbName);
                return;
            }

            final String quoted = "\"" + dbName.replace("\"", "\"\"") + "\"";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE " + quoted);
            }
            LOGGER.info("PostgreSQL database '{}' created", dbName);
        }
    }

    private static boolean databaseExists(final Connection conn, final String dbName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
