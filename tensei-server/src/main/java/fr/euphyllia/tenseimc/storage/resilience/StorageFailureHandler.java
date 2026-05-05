package fr.euphyllia.tenseimc.storage.resilience;

public interface StorageFailureHandler {

    void onSaveFailure(SaveFailureContext context);

    void onLoadFailure(LoadFailureContext context);
}
