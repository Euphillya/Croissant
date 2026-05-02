package fr.euphyllia.tenseimc.configuration;

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
import static io.papermc.paper.configuration.PaperConfigurations.SPIGOT_WORLD_CONFIG_CONTEXT_KEY;

public class TenseiConfigHandlers extends Configurations<TenseiConfigGlobal, TenseiConfigWorld> {

    static final String GLOBAL = "tensei-global.yml";
    static final String DEFAULT_WORLD = "tensei-world.yml";
    public static final ComponentLogger LOGGER = ComponentLogger.logger("TenseiConfigHandlers");
    static final String WORLD = "tensei-world.yml";

    public TenseiConfigHandlers(Path configDir) {
        super(configDir, TenseiConfigGlobal.class, TenseiConfigWorld.class, GLOBAL, DEFAULT_WORLD, WORLD);
    }

    public static TenseiConfigHandlers setup(final Path configDir) throws IOException {
        if (!Files.isDirectory(configDir)) {
            Files.createDirectories(configDir);
        }
        return new TenseiConfigHandlers(configDir);
    }

    public static ContextMap createWorldContextMap(final Path dir, final Identifier worldKey, final SpigotWorldConfig spigotConfig, final RegistryAccess registryAccess, final GameRules gameRules) {
        return ContextMap.builder()
                .put(WORLD_DIRECTORY, dir)
                .put(WORLD_KEY, worldKey)
                .put(SPIGOT_WORLD_CONFIG_CONTEXT_KEY, Suppliers.ofInstance(spigotConfig))
                .put(REGISTRY_ACCESS, registryAccess)
                .put(GAME_RULES, gameRules)
                .build();
    }

    private static ContextMap createWorldContextMap(ServerLevel level) {
        return createWorldContextMap(level.getServer().storageSource.getDimensionPath(level.dimension()), level.dimension().identifier(), level.spigotConfig, level.registryAccess(), level.getGameRules());
    }

    @Override
    protected int globalConfigVersion() {
        return TenseiConfigGlobal.CURRENT_VERSION;
    }

    @Override
    protected int worldConfigVersion() {
        return TenseiConfigWorld.CURRENT_VERSION;
    }

    @Override
    protected boolean isConfigType(@NonNull Type type) {
        return ConfigurationPart.class.isAssignableFrom(erase(type));
    }

    @Override
    public @NonNull TenseiConfigGlobal initializeGlobalConfiguration(@NonNull RegistryAccess registryAccess) throws ConfigurateException {
        TenseiConfigGlobal config = super.initializeGlobalConfiguration(registryAccess);
        TenseiConfigGlobal.set(config);
        return config;
    }

    private Path worldFilePath(ContextMap map) {
        Identifier worldKey = map.require(WORLD_KEY);
        String raw = worldKey.getNamespace() + "_" + worldKey.getPath();
        String safe = raw.replaceAll("[^a-zA-Z0-9._-]", "_");
        return this.globalFolder.resolve("tensei-world-" + safe + ".yml");
    }

    @Override
    public @NonNull TenseiConfigWorld createWorldConfig(final @NonNull ContextMap contextMap) {
        try {
            return super.createWorldConfig(contextMap);
        } catch (IOException e) {
            LOGGER.error("Failed to load world configuration for context: {}", contextMap, e);
            return new TenseiConfigWorld();
        }
    }

    private TenseiConfigWorld createWorldConfigInConfigFolder(
            final @NonNull ContextMap contextMap,
            final CheckedFunction<ConfigurationNode, TenseiConfigWorld, SerializationException> creator
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
                    reloader(this.globalConfigClass, TenseiConfigGlobal.get())
            );

            this.initializeWorldDefaultsConfiguration(server.registryAccess());

            for (ServerLevel level : server.getAllLevels()) {
                this.createWorldConfig(
                        createWorldContextMap(level),
                        reloader(this.worldConfigClass, level.tensei)
                );
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to reload Tensei configurations", exception);
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
