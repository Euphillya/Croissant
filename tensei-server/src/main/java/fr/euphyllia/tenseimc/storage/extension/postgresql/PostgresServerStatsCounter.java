package fr.euphyllia.tenseimc.storage.extension.postgresql;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.database.postgres.PostgresManager;
import fr.euphyllia.tenseimc.storage.database.postgres.PostgresStatsAccess;
import fr.euphyllia.tenseimc.storage.exception.StorageException;
import fr.euphyllia.tenseimc.storage.model.SaveOperation;
import fr.euphyllia.tenseimc.storage.resilience.SaveFailureContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class PostgresServerStatsCounter extends ServerStatsCounter {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path DUMMY_PATH = Path.of("/dev/null");

    private final UUID uuid;
    private final String playerName;
    private final DataFixer dataFixer;

    public PostgresServerStatsCounter(
            final MinecraftServer server,
            final UUID uuid,
            final String playerName,
            final DataFixer dataFixer
    ) {
        super(server, DUMMY_PATH);
        this.uuid = uuid;
        this.playerName = playerName;
        this.dataFixer = dataFixer;

        loadFromPostgres();
    }

    private void loadFromPostgres() {
        try {
            final Optional<JsonElement> jsonOpt = PostgresStatsAccess.load(uuid);
            if (jsonOpt.isPresent()) {
                final JsonElement element = jsonOpt.get();
                if (element.isJsonNull()) {
                    LOGGER.warn("Stats data is JSON null for {}", uuid);
                    return;
                }
                this.parse(this.dataFixer, element);
            }
        } catch (final StorageException ex) {
            LOGGER.error("Failed to load stats from Postgres for {}", uuid, ex);
        } catch (final Exception ex) {
            LOGGER.error("Unexpected error loading stats for {}", uuid, ex);
        }
    }

    @Override
    public void save() {
        if (org.spigotmc.SpigotConfig.disableStatSaving) return;

        final JsonElement json;
        try {
            json = this.toJson();
        } catch (final Exception ex) {
            LOGGER.error("Failed to encode stats for {}", uuid, ex);
            return;
        }

        PostgresManager.ioExecutor().execute(() -> {
            try {
                PostgresStatsAccess.save(uuid, json);
            } catch (final StorageException ex) {
                PostgresManager.failureHandler().onSaveFailure(
                        new SaveFailureContext(uuid, playerName, SaveOperation.AUTOSAVE, ex, 1)
                );
            }
        });
    }
}
