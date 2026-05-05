package fr.euphyllia.tenseimc.storage.exception;

public class TransientStorageException extends StorageException {

    public TransientStorageException(final String message) {
        super(message);
    }

    public TransientStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
