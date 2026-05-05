package fr.euphyllia.tenseimc.storage.database.postgres.util;

public class Identifiers {

    private Identifiers() {
    }

    public static String sanitize(final String ident) {
        if (ident == null) return "";
        return ident.replaceAll("[^a-zA-Z0-9_]", "");
    }

}
