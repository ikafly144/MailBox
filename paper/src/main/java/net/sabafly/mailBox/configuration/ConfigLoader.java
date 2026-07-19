package net.sabafly.mailBox.configuration;

import io.papermc.paper.configuration.type.Duration;
import io.papermc.paper.configuration.type.DurationOrDisabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
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
                                .register(Duration.SERIALIZER)
                                .register(DurationOrDisabled.SERIALIZER)
                                .build())
                )
                .build();
        try {
            if (dataDir.resolve("config.yml").toFile().exists()) {
                var root = loader.load();
                this.config = root.get(Config.class);
                // 兼容旧配置：移除已废弃的 messages 区块
                if (root.node("messages").virtual() == false) {
                    logger.warn("config.yml 中的 messages 区块已废弃，请改用 locales/*.yml 语言文件。已自动忽略该区块。");
                    root.node("messages").set(null);
                    loader.save(root);
                }
            } else {
                this.config = new Config();
                var root = loader.createNode(loader.defaultOptions()).set(Config.class, this.config);
                root.node("messages").set(null);
                loader.save(root);
            }
        } catch (Exception e) {
            logger.error("Failed to load config", e);
        }

        // Apply locale overrides
        localeManager.applyLocale(this.config);
    }

}
