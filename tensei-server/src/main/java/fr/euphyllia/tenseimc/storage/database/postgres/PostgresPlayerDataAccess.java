package fr.euphyllia.tenseimc.storage.database.postgres;

import fr.euphyllia.tenseimc.storage.database.postgres.util.SQLExecute;
import fr.euphyllia.tenseimc.storage.exception.PermanentStorageException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PostgresPlayerDataAccess {

    private PostgresPlayerDataAccess() {
    }

    private static String upsertSql() {
        return """
                INSERT INTO %s.player_data (uuid, name, data, data_version, updated_at)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (uuid) DO UPDATE SET
                    name = EXCLUDED.name,
                    data = EXCLUDED.data,
                    data_version = EXCLUDED.data_version,
                    updated_at = NOW()
                """.formatted(PostgresManager.schema());
    }

    private static String selectSql() {
        return "SELECT data FROM %s.player_data WHERE uuid = ?".formatted(PostgresManager.schema());
    }

    public static void save(final UUID uuid, final String name, final CompoundTag tag, final boolean useGzip) {
        final byte[] payload;
        try {
            payload = encodeNbt(tag, useGzip);
        } catch (final IOException ex) {
            throw new PermanentStorageException("Failed to encode NBT for " + uuid, ex);
        }
        final int dataVersion = NbtUtils.getDataVersion(tag);

        SQLExecute.update(
                "save player " + uuid,
                upsertSql(),
                List.of(uuid, name, payload, dataVersion)
        );
    }

    public static Optional<CompoundTag> load(final UUID uuid) {
        return Optional.ofNullable(SQLExecute.queryMapOnLogin(
                "load player " + uuid,
                selectSql(),
                List.of(uuid),
                rs -> {
                    try {
                        if (!rs.next()) return null;
                        final byte[] payload = rs.getBytes("data");
                        if (payload == null || payload.length == 0) return null;
                        return decodeNbt(payload);
                    } catch (final Exception ex) {
                        throw new PermanentStorageException("Corrupted NBT in DB for " + uuid, ex);
                    }
                }
        ));
    }

    private static byte[] encodeNbt(final CompoundTag tag, final boolean useGzip) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (useGzip) {
                try (DataOutputStream dos = new DataOutputStream(baos)) {
                    NbtIo.writeCompressed(tag, dos);
                }
            } else {
                try (DataOutputStream dos = new DataOutputStream(baos)) {
                    NbtIo.write(tag, dos);
                }
            }
            return baos.toByteArray();
        }
    }

    private static CompoundTag decodeNbt(final byte[] payload) throws IOException {
        final boolean isGzip = payload.length >= 2
                && payload[0] == (byte) 0x1F
                && payload[1] == (byte) 0x8B;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(payload)) {
            if (isGzip) {
                return NbtIo.readCompressed(bais, NbtAccounter.unlimitedHeap());
            } else {
                try (DataInputStream dis = new DataInputStream(bais)) {
                    return NbtIo.read(dis, NbtAccounter.unlimitedHeap());
                }
            }
        }
    }
}
