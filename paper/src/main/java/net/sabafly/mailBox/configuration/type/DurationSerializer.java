package net.sabafly.mailBox.configuration.type;

import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

public class DurationSerializer extends ScalarSerializer<Duration> {

    public static final DurationSerializer SERIALIZER = new DurationSerializer();

    private DurationSerializer() {
        super(Duration.class);
    }

    public Duration deserialize(@NonNull Type type, Object obj) throws SerializationException {
        return Duration.of(obj.toString());
    }

    protected @NonNull Object serialize(Duration item, @NonNull Predicate<Class<?>> typeSupported) {
        return item.value();
    }
}
