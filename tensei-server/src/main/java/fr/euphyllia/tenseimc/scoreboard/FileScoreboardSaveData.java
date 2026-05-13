package fr.euphyllia.tenseimc.scoreboard;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public final class FileScoreboardSaveData extends ScoreboardSaveData {

    public static final String FILE_NAME = "scoreboard.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(FileScoreboardSaveData.class);
    private final Path file;
    private final Path tmpFile;
    private final ReentrantLock writeLock = new ReentrantLock();

    private FileScoreboardSaveData(final Path file, final ScoreboardSaveData.Packed initial) {
        super();
        super.setData(initial);
        this.file = file;
        this.tmpFile = file.resolveSibling(FILE_NAME + ".tmp");
    }

    public static FileScoreboardSaveData loadOrCreate(final Path baseDir) {
        try {
            Files.createDirectories(baseDir);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to create scoreboard dir: " + baseDir, ex);
        }
        final Path file = baseDir.resolve(FILE_NAME);
        final ScoreboardSaveData.Packed initial =
                readFile(file).orElseGet(FileScoreboardSaveData::emptyPacked);
        return new FileScoreboardSaveData(file, initial);
    }

    private static Optional<Packed> readFile(final Path file) {
        if (!Files.exists(file)) return Optional.empty();
        try {
            final String text = Files.readString(file, StandardCharsets.UTF_8);
            if (text.isBlank()) return Optional.empty();
            final JsonElement json = JsonParser.parseString(text);
            return ScoreboardSaveData.Packed.CODEC
                    .parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> LOGGER.warn("Codec decode error reading {}: {}", file, err));
        } catch (final IOException ex) {
            LOGGER.error("Failed to read scoreboard file {}", file, ex);
            return Optional.empty();
        } catch (final RuntimeException ex) {
            LOGGER.error("Corrupted scoreboard JSON at {}, ignoring", file, ex);
            return Optional.empty();
        }
    }

    private static ScoreboardSaveData.Packed emptyPacked() {
        return new ScoreboardSaveData.Packed(List.of(), List.of(), Map.of(), List.of());
    }

    @Override
    public @NonNull ScoreboardSaveData setData(final ScoreboardSaveData.@NonNull Packed data) {
        super.setData(data);
        try {
            writeFile(data);
        } catch (final Exception ex) {
            LOGGER.error("Failed to write scoreboard JSON to {}", this.file, ex);
        }
        return this;
    }

    private void writeFile(final ScoreboardSaveData.Packed data) throws IOException {
        final JsonElement json = ScoreboardSaveData.Packed.CODEC
                .encodeStart(JsonOps.INSTANCE, data)
                .resultOrPartial(err -> LOGGER.warn("Codec encode error: {}", err))
                .orElse(null);

        if (json == null) {
            LOGGER.error("Failed to encode scoreboard, skipping write");
            return;
        }

        final String text = new GsonBuilder().setPrettyPrinting().create().toJson(json);

        this.writeLock.lock();
        try {
            Files.writeString(this.tmpFile, text, StandardCharsets.UTF_8);
            try {
                Files.move(this.tmpFile, this.file,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException atomicFailed) {
                Files.move(this.tmpFile, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(this.tmpFile);
            } catch (final IOException ignored) {
            }
            this.writeLock.unlock();
        }
    }
}
