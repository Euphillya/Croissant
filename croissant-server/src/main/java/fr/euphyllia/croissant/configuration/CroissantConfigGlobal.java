package fr.euphyllia.croissant.configuration;

import io.papermc.paper.configuration.ConfigurationPart;

@SuppressWarnings({"CanBeFinal", "FieldCanBeLocal", "FieldMayBeFinal", "InnerClassMayBeStatic"})
public class CroissantConfigGlobal extends ConfigurationPart {

    static CroissantConfigGlobal instance;

    static final int CURRENT_VERSION = 1;

    static void set(final CroissantConfigGlobal config) {
        instance = config;
    }

    public static CroissantConfigGlobal get() {
        return instance;
    }



}
