package net.sabafly.mailbox.configurations.transformation;

import net.sabafly.mailbox.configurations.ConfigurationPart;
import net.sabafly.mailbox.utils.LogUtil;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import java.util.logging.Logger;

public class Transformation {

    private static final Logger LOGGER = LogUtil.getLogger();

    private Transformation() {
        throw new IllegalStateException("Utility class");
    }

    public static ConfigurationTransformation.Versioned create() {
        return ConfigurationTransformation.versionedBuilder()
                .versionKey(ConfigurationPart.VERSION_FIELD)
                .addVersion(1, Transformations.initiate())
                .addVersion(2, Transformations.addMessages())
                .addVersion(3, Transformations.version3())
                .build();
    }

    public static <N extends ConfigurationNode> void updateNode(final N node) throws ConfigurateException {
        if (!node.virtual()) { // we only want to migrate existing data
            final ConfigurationTransformation.Versioned trans = create();
            final int startVersion = trans.version(node);
            trans.apply(node);
            final int endVersion = trans.version(node);
            if (startVersion != endVersion) { // we might not have made any changes
                LOGGER.info("Updated config schema from " + startVersion + " to " + endVersion);
            }
        }
    }

}
