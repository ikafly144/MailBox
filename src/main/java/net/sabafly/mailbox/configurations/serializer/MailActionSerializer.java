package net.sabafly.mailbox.configurations.serializer;

import net.sabafly.mailbox.type.MailAction;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import javax.annotation.Nullable;
import java.lang.reflect.Type;

public class MailActionSerializer implements TypeSerializer<MailAction> {
    @Override
    public MailAction deserialize(Type type, ConfigurationNode node) throws SerializationException {
        var string = node.getString();
        return string != null ? MailAction.deserialize(string) : null;
    }

    @Override
    public void serialize(Type type, @Nullable MailAction obj, ConfigurationNode node) {
        if (obj != null) {
            node.raw(MailAction.serialize(obj));
        }
    }
}
