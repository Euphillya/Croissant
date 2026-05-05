package fr.euphyllia.tenseimc.storage.config;

import fr.euphyllia.tenseimc.configuration.TenseiConfigGlobal;

public final class StorageConfigMapper {

    private StorageConfigMapper() {
    }

    public static StorageConfig toStorageConfig(final TenseiConfigGlobal.Storage cfg) {
        return new StorageConfig(
                switch (cfg.backend) {
                    case FILE -> StorageConfig.Backend.FILE;
                    case POSTGRESQL -> StorageConfig.Backend.POSTGRESQL;
                },
                cfg.serverId,
                new StorageConfig.Postgres(
                        cfg.postgres.hostname,
                        cfg.postgres.port,
                        cfg.postgres.database,
                        cfg.postgres.schema,
                        cfg.postgres.username,
                        cfg.postgres.password,
                        cfg.postgres.useGzip,
                        new StorageConfig.Postgres.Pool(
                                cfg.postgres.pool.loginPoolSize,
                                cfg.postgres.pool.savePoolSize,
                                cfg.postgres.pool.connectionTimeoutMs,
                                cfg.postgres.pool.idleTimeoutMs,
                                cfg.postgres.pool.maxLifetimeMs,
                                cfg.postgres.pool.keepaliveTimeMs
                        ),
                        new StorageConfig.Postgres.Lock(
                                cfg.postgres.lock.ttlSeconds,
                                cfg.postgres.lock.heartbeatIntervalMs
                        )
                ),
                new StorageConfig.Resilience(
                        new StorageConfig.Resilience.Login(
                                cfg.resilience.login.maxRetries,
                                cfg.resilience.login.retryDelayMs,
                                cfg.resilience.login.unavailableKickMessage,
                                cfg.resilience.login.alreadyConnectedKickMessage
                        ),
                        new StorageConfig.Resilience.Autosave(
                                switch (cfg.resilience.autosave.mode) {
                                    case STRICT_KICK -> StorageConfig.Resilience.FailureMode.STRICT_KICK;
                                    case RETRY_THEN_KICK -> StorageConfig.Resilience.FailureMode.RETRY_THEN_KICK;
                                    case BUFFERED -> StorageConfig.Resilience.FailureMode.BUFFERED;
                                },
                                cfg.resilience.autosave.kickMessage,
                                cfg.resilience.autosave.maxRetries,
                                cfg.resilience.autosave.initialBackoffMs,
                                cfg.resilience.autosave.maxBackoffMs,
                                cfg.resilience.autosave.bufferMaxSize,
                                cfg.resilience.autosave.bufferMaxAgeMs
                        ),
                        new StorageConfig.Resilience.Shutdown(
                                cfg.resilience.shutdown.flushTimeoutMs,
                                cfg.resilience.shutdown.walPath
                        )
                )
        );
    }
}