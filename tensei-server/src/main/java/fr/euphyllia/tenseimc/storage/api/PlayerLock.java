package fr.euphyllia.tenseimc.storage.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerLock {

    CompletableFuture<Boolean> tryAcquire(UUID playerUuid);

    CompletableFuture<Boolean> refresh(UUID playerUuid);

    CompletableFuture<Void> release(UUID playerUuid);
}
