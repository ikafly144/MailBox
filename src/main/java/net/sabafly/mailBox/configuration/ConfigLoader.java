package net.sabafly.mailBox.configuration;

import io.papermc.paper.configuration.type.Duration;
import io.papermc.paper.configuration.type.DurationOrDisabled;
import net.sabafly.mailBox.MailBox;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.util.MapFactories;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public class ConfigLoader {

    private Config config;
    private final Path dataDir;

    public ConfigLoader(Path dataDir) {
        this.config = new Config();
        this.dataDir = dataDir;
    }

    public Config config() {
        return this.config;
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
                this.config = loader.load().get(Config.class);
                loader.save(loader.createNode(loader.defaultOptions()).set(Config.class, this.config));
            } else {
                this.config = new Config();
                loader.save(loader.createNode(loader.defaultOptions()).set(Config.class, this.config));
            }
        } catch (Exception e) {
            MailBox.getInstance().getSLF4JLogger().error("Failed to load config", e);
        }
    }

}
