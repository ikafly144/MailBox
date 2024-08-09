package net.sabafly.mailbox.configurations.transformation;

import net.sabafly.mailbox.configurations.PluginConfiguration;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public class Transformations {

    private Transformations() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    // version 1
    static ConfigurationTransformation initiate() {
        return ConfigurationTransformation.builder()
                .addAction(NodePath.path(), (path, value) -> {
                    value.set(PluginConfiguration.class, new PluginConfiguration());
                    return null;
                })
                .build();
    }

    // version 2
    static ConfigurationTransformation addMessages() {
        return ConfigurationTransformation.builder()
                .addAction(NodePath.path(), (path, value) -> {
                    value.node("messages").set(PluginConfiguration.Messages.class, new PluginConfiguration.Messages());
                    return null;
                })
                .build();
    }

    // version 3
    static ConfigurationTransformation version3() {
        return ConfigurationTransformation.builder()
                .addAction(NodePath.path("messages", "next-page"), (path, value) -> new Object[]{"messages", "change-previous-page"})
                .addAction(NodePath.path("messages", "previous-page"), (path, value) -> new Object[]{"messages", "change-next-page"})
                .build();
    }

}
