package fr.euphyllia.tenseimc.storage.config;

public record StorageConfig(
        Backend backend,
        String serverId,
        Postgres postgres,
        Resilience resilience
) {

    public enum Backend {
        FILE, POSTGRESQL
    }

    public record Postgres(
            String hostname,
            int port,
            String database,
            String schema,
            String username,
            String password,
            boolean useGzip,
            Pool pool,
            Lock lock
    ) {
        public record Pool(
                int loginPoolSize,
                int savePoolSize,
                long connectionTimeoutMs,
                long idleTimeoutMs,
                long maxLifetimeMs,
                long keepaliveTimeMs
        ) {
        }

        public record Lock(
                long ttlSeconds,
                long heartbeatIntervalMs
        ) {
        }

        public String jdbcUrl() {
            return "jdbc:postgresql://%s:%d/%s".formatted(hostname, port, database);
        }

        public String bootstrapJdbcUrl() {
            return "jdbc:postgresql://%s:%d/postgres".formatted(hostname, port);
        }
    }

    public record Resilience(
            Login login,
            Autosave autosave,
            Shutdown shutdown,
            Health health
    ) {

        public enum FailureMode {
            STRICT_KICK,
            RETRY_THEN_KICK,
            BUFFERED
        }

        public record Login(
                int maxRetries,
                long retryDelayMs,
                String unavailableKickMessage,
                String alreadyConnectedKickMessage
        ) {
        }

        public record Autosave(
                FailureMode mode,
                String kickMessage,
                int maxRetries,
                long initialBackoffMs,
                long maxBackoffMs,
                int bufferMaxSize,
                long bufferMaxAgeMs
        ) {
        }

        public record Shutdown(
                long flushTimeoutMs,
                String walPath
        ) {
        }

        public record Health(
                long checkIntervalMs,
                int failureThreshold,
                int recoveryThreshold,
                String dbDownKickMessage,
                String dbDownLoginMessage
        ) {
        }
    }
}
