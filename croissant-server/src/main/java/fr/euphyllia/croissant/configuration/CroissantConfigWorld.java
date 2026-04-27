package fr.euphyllia.croissant.configuration;

import io.papermc.paper.configuration.Configuration;
import io.papermc.paper.configuration.ConfigurationPart;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "NotNullFieldNotInitialized", "InnerClassMayBeStatic"})
public class CroissantConfigWorld extends ConfigurationPart {

    static final int CURRENT_VERSION = 1;

    @Setting(Configuration.VERSION_FIELD)
    public int version = CURRENT_VERSION;


}
