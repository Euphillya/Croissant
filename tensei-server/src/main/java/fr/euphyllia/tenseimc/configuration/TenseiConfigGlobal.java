package fr.euphyllia.tenseimc.configuration;

import io.papermc.paper.configuration.ConfigurationPart;

@SuppressWarnings({"CanBeFinal", "FieldCanBeLocal", "FieldMayBeFinal", "InnerClassMayBeStatic"})
public class TenseiConfigGlobal extends ConfigurationPart {

    static TenseiConfigGlobal instance;

    static final int CURRENT_VERSION = 1;

    static void set(final TenseiConfigGlobal config) {
        instance = config;
    }

    public static TenseiConfigGlobal get() {
        return instance;
    }


    public Storage storage = new Storage();

    public class Storage extends ConfigurationPart {

        public Backend backend = Backend.FILE;
        public String serverId = "";

        public Postgres postgres = new Postgres();
        public Resilience resilience = new Resilience();

        public enum Backend {
            FILE,
            POSTGRESQL
        }

        public class Postgres extends ConfigurationPart {
            public String hostname = "localhost";
            public int port = 5432;
            public String database = "tenseimc";
            public String schema = "public";
            public String username = "tenseimc";
            public String password = "";
            public boolean useGzip = true;
            public Pool pool = new Pool();
            public Lock lock = new Lock();

            public class Pool extends ConfigurationPart {
                public int loginPoolSize = 5;
                public int savePoolSize = 10;
                public long connectionTimeoutMs = 2000L;
                public long idleTimeoutMs = 600_000L;
                public long maxLifetimeMs = 1_800_000L;
                public long keepaliveTimeMs = 30_000L;
            }


            public class Lock extends ConfigurationPart {
                public long ttlSeconds = 600L;
                public long heartbeatIntervalMs = 60_000L;
            }
        }

        public class Resilience extends ConfigurationPart {
            public Login login = new Login();
            public Autosave autosave = new Autosave();
            public Shutdown shutdown = new Shutdown();

            public class Login extends ConfigurationPart {
                public int maxRetries = 2;
                public long retryDelayMs = 500L;
                public String unavailableKickMessage =
                        "<red>Service de données indisponible, réessayez dans quelques instants.";
                public String alreadyConnectedKickMessage =
                        "<red>Vous êtes déjà connecté sur un autre serveur.";
            }

            public class Autosave extends ConfigurationPart {
                public FailureMode mode = FailureMode.STRICT_KICK;
                public String kickMessage =
                        "<red>Erreur de sauvegarde, déconnexion pour préserver vos données.";
                public int maxRetries = 3;
                public long initialBackoffMs = 1000L;
                public long maxBackoffMs = 10_000L;
                public int bufferMaxSize = 100;
                public long bufferMaxAgeMs = 60_000L;
            }

            public class Shutdown extends ConfigurationPart {
                public long flushTimeoutMs = 30_000L;
                public String walPath = "data/storage-wal";
            }

            public enum FailureMode {
                STRICT_KICK,
                RETRY_THEN_KICK,
                BUFFERED
            }
        }
    }


}
