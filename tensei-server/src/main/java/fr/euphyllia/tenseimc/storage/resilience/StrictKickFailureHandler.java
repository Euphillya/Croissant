package fr.euphyllia.tenseimc.storage.resilience;

import com.mojang.logging.LogUtils;
import fr.euphyllia.tenseimc.storage.model.SaveOperation;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.connection.DisconnectionReason;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.UUID;

public final class StrictKickFailureHandler implements StorageFailureHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final MinecraftServer server;
    private final Component saveKickMessage;

    public StrictKickFailureHandler(final MinecraftServer server, final String saveKickMessageRaw) {
        this.server = server;
        this.saveKickMessage = parseMessage(saveKickMessageRaw);
    }


    @Override
    public void onSaveFailure(final SaveFailureContext context) {
        if (context.operation() == SaveOperation.LOGOUT || context.operation() == SaveOperation.SHUTDOWN) {
            LOGGER.error(
                    "Save failed for {} ({}) during {} - data may be lost. Consider BUFFERED mode for shutdown safety.",
                    context.playerName(), context.playerUuid(), context.operation(), context.cause()
            );
            return;
        }

        LOGGER.error(
                "Save failed for {} ({}) during {} (attempt #{}), kicking immediately.",
                context.playerName(), context.playerUuid(), context.operation(),
                context.attemptNumber(), context.cause()
        );
        kickIfOnline(context.playerUuid(), this.saveKickMessage);
    }

    @Override
    public void onLoadFailure(final LoadFailureContext context) {
        LOGGER.error("Load failed for {} ({}), kicking.",
                context.playerName(), context.playerUuid(), context.cause());
        kickIfOnline(context.playerUuid(), this.saveKickMessage);
    }

    private void kickIfOnline(final UUID uuid, final Component reason) {
        this.server.execute(() -> {
            final ServerPlayer player = this.server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                player.connection.disconnect(PaperAdventure.asVanilla(reason), DisconnectionReason.UNKNOWN);
            }
        });
    }

    private static Component parseMessage(final String raw) {
        try {
            return MiniMessage.miniMessage().deserialize(raw);
        } catch (final Exception ex) {
            LOGGER.warn("Failed to parse kick message '{}', using fallback", raw, ex);
            return Component.text(raw);
        }
    }
}
