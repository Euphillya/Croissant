package fr.euphyllia.tenseimc.storage.exception;

public class PermanentStorageException extends StorageException {

    public PermanentStorageException(final String message) {
        super(message);
    }

    public PermanentStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
