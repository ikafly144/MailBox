package net.sabafly.mailBox.configuration;

import net.sabafly.mailBox.configuration.type.DurationOrDisabledSerializer;
import net.sabafly.mailBox.configuration.type.DurationSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private Config config;
    private final Path dataDir;
    private final LocaleManager localeManager;

    public ConfigLoader(Path dataDir) {
        this.config = new Config();
        this.dataDir = dataDir;
        this.localeManager = new LocaleManager(dataDir);
    }

    public Config config() {
        return this.config;
    }

    public LocaleManager localeManager() {
        return this.localeManager;
    }

    public void reload() {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(this.dataDir.resolve("config.yml"))
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .defaultOptions(options -> options
                        .shouldCopyDefaults(true)
                        .mapFactory(MapFactories.insertionOrdered())
                        .serializers(builder -> builder
                                .register(DurationSerializer.SERIALIZER)
                                .register(DurationOrDisabledSerializer.SERIALIZER)
                                .build())
                )
                .build();
        try {
            if (dataDir.resolve("config.yml").toFile().exists()) {
                var root = loader.load();
                // Legacy migration: remove deprecated messages block from config.yml
                if (!root.node("messages").virtual()) {
                    logger.warn("The 'messages' block in config.yml is deprecated.");

                    final var messages = root.node("messages").get(Messages.class);

                    if (messages != null) {
                        this.localeManager.saveLocale(messages, Locale.CUSTOM);
                        logger.warn("The 'messages' block in config.yml has been migrated to locales/custom.yml. Please remove the 'messages' block from config.yml.");
                    }

                    root.removeChild("messages");
                    root.node("locale").set(Locale.CUSTOM);
                    loader.save(root);
                }
                this.config = root.get(Config.class, new Config());
                Locale locale = Locale.fromString(this.config.locale);
                localeManager.loadLocale(locale);
            } else {
                this.config = new Config();
            }
        } catch (Exception e) {
            logger.error("Failed to load config", e);
        }
    }

}
