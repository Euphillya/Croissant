package fr.euphyllia.tenseimc.storage.database.postgres.util;

import fr.euphyllia.tenseimc.storage.exception.PermanentStorageException;
import fr.euphyllia.tenseimc.storage.exception.StorageException;
import fr.euphyllia.tenseimc.storage.exception.TransientStorageException;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;

public class SqlExceptionTranslator {
    private SqlExceptionTranslator() {
    }

    public static StorageException translate(final String operation, final SQLException ex) {
        if (ex instanceof SQLTransientException) {
            return new TransientStorageException(operation + " (transient SQL error)", ex);
        }
        if (ex instanceof SQLNonTransientException) {
            return new PermanentStorageException(operation + " (permanent SQL error)", ex);
        }

        final String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.length() >= 2) {
            final String sqlClass = sqlState.substring(0, 2);

            switch (sqlClass) {
                case "08", "40", "53", "57", "58":
                    return new TransientStorageException(operation + " (sqlstate " + sqlState + ")", ex);
                default:
                    break;
            }

            switch (sqlClass) {
                case "22", "23", "42":
                    return new PermanentStorageException(operation + " (sqlstate " + sqlState + ")", ex);
                default:
                    break;
            }
        }

        return new TransientStorageException(operation + " (uncategorized SQL error)", ex);
    }
}
