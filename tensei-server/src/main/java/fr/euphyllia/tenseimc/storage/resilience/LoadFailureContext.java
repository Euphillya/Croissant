package fr.euphyllia.tenseimc.storage.resilience;

import fr.euphyllia.tenseimc.storage.exception.StorageException;

import java.util.UUID;

public record LoadFailureContext(
        UUID playerUuid,
        String playerName,
        StorageException cause
) {
}
