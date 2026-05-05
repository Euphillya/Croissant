package fr.euphyllia.tenseimc.storage.resilience;

import fr.euphyllia.tenseimc.storage.exception.StorageException;
import fr.euphyllia.tenseimc.storage.model.SaveOperation;

import java.util.UUID;

public record SaveFailureContext(
        UUID playerUuid,
        String playerName,
        SaveOperation operation,
        StorageException cause,
        int attemptNumber
) {
}

