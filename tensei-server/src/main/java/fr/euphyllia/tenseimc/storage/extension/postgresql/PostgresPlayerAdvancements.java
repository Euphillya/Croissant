package fr.euphyllia.tenseimc.storage.extension.postgresql;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import fr.euphyllia.tenseimc.storage.database.postgres.PostgresAdvancementsAccess;
import fr.euphyllia.tenseimc.storage.database.postgres.PostgresManager;
import fr.euphyllia.tenseimc.storage.exception.StorageException;
import fr.euphyllia.tenseimc.storage.model.SaveOperation;
import fr.euphyllia.tenseimc.storage.resilience.SaveFailureContext;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class PostgresPlayerAdvancements extends PlayerAdvancements {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path DUMMY_PATH = Path.of("/dev/null");

    public PostgresPlayerAdvancements(
            final DataFixer dataFixer,
            final PlayerList playerList,
            final ServerAdvancementManager manager,
            final ServerPlayer player
    ) {
        super(dataFixer, playerList, manager, DUMMY_PATH, player);
    }

    @Override
    public void load(final @NotNull ServerAdvancementManager manager) {
        final UUID uuid = this.player.getUUID();

        try {
            final Optional<JsonElement> jsonOpt = PostgresAdvancementsAccess.load(uuid);
            if (jsonOpt.isPresent()) {
                final PlayerAdvancements.Data data = this.codec
                        .parse(JsonOps.INSTANCE, jsonOpt.get())
                        .getOrThrow(JsonParseException::new);
                this.applyFrom(manager, data);
            }
        } catch (final StorageException ex) {
            LOGGER.error("Failed to load advancements from Postgres for {}", uuid, ex);
        } catch (final Exception ex) {
            LOGGER.error("Unexpected error loading advancements for {}", uuid, ex);
        }

        this.checkForAutomaticTriggers(manager);
        this.registerListeners(manager);
    }

    @Override
    public void save() {
        if (org.spigotmc.SpigotConfig.disableAdvancementSaving) return;

        final UUID uuid = this.player.getUUID();
        final String name = this.player.getPlainTextName();

        final JsonElement json;
        try {
            json = this.codec.encodeStart(JsonOps.INSTANCE, this.asData()).getOrThrow();
        } catch (final Exception ex) {
            LOGGER.error("Failed to encode advancements for {}", uuid, ex);
            return;
        }

        PostgresManager.submitSave(() -> {
            try {
                PostgresAdvancementsAccess.save(uuid, json);
            } catch (final StorageException ex) {
                PostgresManager.failureHandler().onSaveFailure(
                        new SaveFailureContext(uuid, name, SaveOperation.AUTOSAVE, ex, 1)
                );
            }
        });
    }
}
