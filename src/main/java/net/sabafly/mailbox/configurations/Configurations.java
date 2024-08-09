package net.sabafly.mailbox.configurations;

import net.sabafly.mailbox.configurations.serializer.LocalDateTimeSerializer;
import net.sabafly.mailbox.configurations.serializer.MailActionSerializer;
import net.sabafly.mailbox.configurations.transformation.Transformation;
import net.sabafly.mailbox.type.MailAction;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Configurations {

    private Configurations() {
        throw new IllegalStateException("Utility class");
    }

    public static void loadPluginConfiguration(Path path) throws ConfigurateException {
        final ConfigurationLoader<?> load = createDefaultConfigLoader(path);
        ConfigurationNode node;
        if (Files.notExists(path)) {
            node = CommentedConfigurationNode.root(load.defaultOptions());
            node.node(ConfigurationPart.VERSION_FIELD).raw(PluginConfiguration.CURRENT_VERSION);
            node.set(PluginConfiguration.class, new PluginConfiguration());
        } else {
            node = load.load();
        }
        Transformation.updateNode(node);
        load.save(node);
        final var config = node.get(PluginConfiguration.class);
        PluginConfiguration.set(config != null ? config : new PluginConfiguration());
    }

    public static void savePluginConfiguration(Path path) throws ConfigurateException {
        final ConfigurationLoader<?> save = createDefaultConfigLoader(path);
        var node = CommentedConfigurationNode.root(save.defaultOptions());
        node.set(PluginConfiguration.get());
        save.save(node);
    }

    private static ConfigurationLoader<?> createDefaultConfigLoader(Path path) {
        return YamlConfigurationLoader.builder()
                .indent(2)
                .path(path)
                .defaultOptions(options -> options
                        .serializers(builder -> builder.register(LocalDateTime.class, new LocalDateTimeSerializer()))
                        .serializers(builder -> builder.register(MailAction.class, new MailActionSerializer()))
                        .shouldCopyDefaults(true))
                .nodeStyle(NodeStyle.BLOCK)
                .build();
    }

}
