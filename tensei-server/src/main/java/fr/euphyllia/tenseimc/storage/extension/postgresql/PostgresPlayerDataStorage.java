package fr.euphyllia.tenseimc.storage.extension.postgresql;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.database.postgres.PostgresManager;
import fr.euphyllia.tenseimc.storage.database.postgres.PostgresPlayerDataAccess;
import fr.euphyllia.tenseimc.storage.exception.StorageException;
import fr.euphyllia.tenseimc.storage.model.SaveOperation;
import fr.euphyllia.tenseimc.storage.resilience.SaveFailureContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public class PostgresPlayerDataStorage extends PlayerDataStorage {

    private static final Logger LOGGER = LogUtils.getLogger();

    public PostgresPlayerDataStorage(
            final LevelStorageSource.LevelStorageAccess levelStorageAccess,
            final DataFixer fixerUpper
    ) {
        super(levelStorageAccess, fixerUpper);
    }

    @Override
    public void save(final @NotNull Player player) {
        saveWithOperation(player, SaveOperation.AUTOSAVE);
    }


    public void saveWithOperation(final Player player, final SaveOperation operation) {
        if (org.spigotmc.SpigotConfig.disablePlayerDataSaving) return;

        final UUID uuid = player.getUUID();
        final String name = player.getPlainTextName();

        final CompoundTag snapshot;
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
            final TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
            player.saveWithoutId(output);
            snapshot = output.buildResult();
        } catch (final Exception ex) {
            LOGGER.error("Failed to serialize NBT for {}", uuid, ex);
            return;
        }

        final boolean useGzip = PostgresManager.config().postgres().useGzip();

        PostgresManager.submitSave(() -> {
            try {
                PostgresPlayerDataAccess.save(uuid, name, snapshot, useGzip);
            } catch (final StorageException ex) {
                PostgresManager.failureHandler().onSaveFailure(
                        new SaveFailureContext(uuid, name, operation, ex, 1)
                );
            }
        });
    }

    @Override
    public @NotNull Optional<CompoundTag> load(final @NotNull NameAndId nameAndId) {
        if (org.spigotmc.SpigotConfig.disablePlayerDataSaving) {
            return super.load(nameAndId);
        }

        final UUID uuid = nameAndId.id();
        final Optional<CompoundTag> result = PostgresPlayerDataAccess.load(uuid);

        return result.map(tag -> {
            final int dataVersion = NbtUtils.getDataVersion(tag);
            return ca.spottedleaf.dataconverter.minecraft.MCDataConverter.convertTag(
                    ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry.PLAYER,
                    tag,
                    dataVersion,
                    ca.spottedleaf.dataconverter.minecraft.util.Version.getCurrentVersion()
            );
        });
    }
}
