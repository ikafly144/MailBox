package net.sabafly.mailbox.configurations.serializer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeSerializer implements TypeSerializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(Type type, ConfigurationNode node) throws SerializationException {
        var string = node.getString();
        return string != null ? LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    @Override
    public void serialize(Type type, @Nullable LocalDateTime obj, ConfigurationNode node) {
        if (obj != null) {
            node.raw(obj.toString());
        }
    }
}
