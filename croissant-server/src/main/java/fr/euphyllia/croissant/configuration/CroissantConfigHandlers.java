package fr.euphyllia.croissant.configuration;

import com.google.common.base.Suppliers;
import io.papermc.paper.configuration.ConfigurationPart;
import io.papermc.paper.configuration.Configurations;
import io.papermc.paper.configuration.PaperConfigurations;
import io.papermc.paper.configuration.mapping.Definition;
import io.papermc.paper.configuration.mapping.FieldProcessor;
import io.papermc.paper.configuration.mapping.InnerClassFieldDiscoverer;
import io.papermc.paper.configuration.mapping.MergeMap;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import org.apache.commons.lang3.SerializationException;
import org.jspecify.annotations.NonNull;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.leangen.geantyref.GenericTypeReflector.erase;

public class CroissantConfigHandlers extends Configurations<CroissantConfigGlobal, CroissantConfigWorld> {

    static final String GLOBAL = "croissant-global.yml";
    static final String DEFAULT_WORLD = "croissant-world.yml";
    public static final ComponentLogger LOGGER = ComponentLogger.logger("CroissantConfigHandlers");

    public CroissantConfigHandlers(Path configDir) {
        super(configDir, CroissantConfigGlobal.class, CroissantConfigWorld.class, GLOBAL, DEFAULT_WORLD, "croissant-world.yml");
    }

    public static CroissantConfigHandlers setup(final Path configDir) throws IOException {
        if (!Files.isDirectory(configDir)) {
            Files.createDirectories(configDir);
        }
        return new CroissantConfigHandlers(configDir);
    }

    public static Configurations.ContextMap createWorldContextMap(
            final String levelName,
            final Identifier worldKey,
            final Path worldDirectory,
            final SpigotWorldConfig spigotConfig,
            final RegistryAccess registryAccess,
            final GameRules gameRules
    ) {
        return Configurations.ContextMap.builder()
                .put(Configurations.WORLD_NAME, levelName)
                .put(Configurations.WORLD_KEY, worldKey)
                .put(Configurations.WORLD_DIRECTORY, worldDirectory)
                .put(PaperConfigurations.SPIGOT_WORLD_CONFIG_CONTEXT_KEY, Suppliers.ofInstance(spigotConfig))
                .put(Configurations.REGISTRY_ACCESS, registryAccess)
                .put(Configurations.GAME_RULES, gameRules)
                .build();
    }

    private static ContextMap createWorldContextMap(ServerLevel level) {
        return createWorldContextMap(
                level.serverLevelData.getLevelName(),
                level.dimension().identifier(),
                level.levelStorageAccess.levelDirectory.path(),
                level.spigotConfig,
                level.registryAccess(),
                level.getGameRules()
        );
    }

    @Override
    protected int globalConfigVersion() {
        return CroissantConfigGlobal.CURRENT_VERSION;
    }

    @Override
    protected int worldConfigVersion() {
        return CroissantConfigWorld.CURRENT_VERSION;
    }

    @Override
    protected boolean isConfigType(@NonNull Type type) {
        return ConfigurationPart.class.isAssignableFrom(erase(type));
    }

    @Override
    public @NonNull CroissantConfigGlobal initializeGlobalConfiguration(@NonNull RegistryAccess registryAccess) throws ConfigurateException {
        CroissantConfigGlobal config = super.initializeGlobalConfiguration(registryAccess);
        CroissantConfigGlobal.set(config);
        return config;
    }

    private Path worldFilePath(ContextMap map) {
        String worldName = map.require(WORLD_NAME);
        String safe = worldName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return this.globalFolder.resolve("croissant-world-" + safe + ".yml");
    }

    @Override
    public @NonNull CroissantConfigWorld createWorldConfig(final @NonNull ContextMap contextMap) {
        try {
            return super.createWorldConfig(contextMap);
        } catch (IOException e) {
            LOGGER.error("Failed to load world configuration for context: {}", contextMap, e);
            return new CroissantConfigWorld();
        }
    }

    private CroissantConfigWorld createWorldConfigInConfigFolder(
            final @NonNull ContextMap contextMap,
            final CheckedFunction<ConfigurationNode, CroissantConfigWorld, SerializationException> creator
    ) throws IOException {
        final Path defaultsFile = this.globalFolder.resolve(this.defaultWorldConfigFileName);
        final YamlConfigurationLoader defaultsLoader = this.createWorldConfigLoaderBuilder(
                this.createDefaultContextMap(contextMap.require(REGISTRY_ACCESS)).build()
        ).defaultOptions(this.applyObjectMapperFactory(this.createWorldObjectMapperFactoryBuilder(
                this.createDefaultContextMap(contextMap.require(REGISTRY_ACCESS)).build()
        ).build())).path(defaultsFile).build();

        final ConfigurationNode defaultsNode = defaultsLoader.load();

        final Path worldFile = worldFilePath(contextMap);
        boolean newFile = Files.notExists(worldFile);
        if (newFile) {
            Files.createDirectories(this.globalFolder);
            Files.createFile(worldFile);
        }

        final YamlConfigurationLoader worldLoader = this.createWorldConfigLoaderBuilder(contextMap)
                .defaultOptions(this.applyObjectMapperFactory(this.createWorldObjectMapperFactoryBuilder(contextMap).build()))
                .path(worldFile)
                .build();

        final ConfigurationNode worldNode = worldLoader.load();
        if (newFile) {
            worldNode.node(io.papermc.paper.configuration.Configuration.VERSION_FIELD).set(this.worldConfigVersion());
        } else {
            this.verifyWorldConfigVersion(contextMap, worldNode);
        }

        this.applyWorldConfigTransformations(contextMap, worldNode, defaultsNode);
        this.applyDefaultsAwareWorldConfigTransformations(contextMap, worldNode, defaultsNode);

        this.trySaveFileNode(worldLoader, worldNode, worldFile.toString());

        worldNode.mergeFrom(defaultsNode);

        return creator.apply(worldNode);
    }

    public void reloadConfigs(final MinecraftServer server) {
        try {
            this.initializeGlobalConfiguration(
                    server.registryAccess(),
                    reloader(this.globalConfigClass, CroissantConfigGlobal.get())
            );

            this.initializeWorldDefaultsConfiguration(server.registryAccess());

            for (ServerLevel level : server.getAllLevels()) {
                this.createWorldConfig(
                        createWorldContextMap(level),
                        reloader(this.worldConfigClass, level.croissant)
                );
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to reload Croissant configurations", exception);
        }
    }

    @Override
    protected ObjectMapper.Factory.@NonNull Builder createGlobalObjectMapperFactoryBuilder() {
        return defaultGlobalFactoryBuilder(super.createGlobalObjectMapperFactoryBuilder());
    }

    private static ObjectMapper.Factory.Builder defaultGlobalFactoryBuilder(ObjectMapper.Factory.Builder builder) {
        return builder.addDiscoverer(io.papermc.paper.configuration.mapping.InnerClassFieldDiscoverer.globalConfig(defaultFieldProcessors()));
    }

    private static List<Definition<? extends Annotation, ?, ? extends FieldProcessor.Factory<?, ?>>> defaultFieldProcessors() {
        return List.of(
                MergeMap.DEFINITION
        );
    }

    @Override
    protected ObjectMapper.Factory.@NonNull Builder createWorldObjectMapperFactoryBuilder(final @NonNull ContextMap contextMap) {
        return super.createWorldObjectMapperFactoryBuilder(contextMap)
                .addDiscoverer(InnerClassFieldDiscoverer.globalConfig(defaultFieldProcessors()));
    }
}
