package fr.euphyllia.tenseimc.configuration;

import io.papermc.paper.configuration.ConfigurationPart;

@SuppressWarnings({"CanBeFinal", "FieldCanBeLocal", "FieldMayBeFinal", "InnerClassMayBeStatic"})
public class TenseiConfigGlobal extends ConfigurationPart {

    static TenseiConfigGlobal instance;

    static final int CURRENT_VERSION = 1;

    static void set(final TenseiConfigGlobal config) {
        instance = config;
    }

    public static TenseiConfigGlobal get() {
        return instance;
    }



}
