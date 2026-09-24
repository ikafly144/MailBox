package net.sabafly.mailBox.configuration.type;

import io.papermc.paper.configuration.type.Duration;
import io.papermc.paper.configuration.type.DurationOrDisabled;
import org.jspecify.annotations.NonNull;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Predicate;

public class DurationOrDisabledSerializer extends ScalarSerializer<DurationOrDisabled> {

    public static final DurationOrDisabledSerializer SERIALIZER = new DurationOrDisabledSerializer();

    private DurationOrDisabledSerializer() {
        super(DurationOrDisabled.class);
    }

    public DurationOrDisabled deserialize(@NonNull Type type, @NonNull Object obj) throws SerializationException {
        if (obj instanceof String string) {
            return "disabled".equalsIgnoreCase(string) ? DurationOrDisabled.USE_DISABLED : new DurationOrDisabled(Optional.of(DurationSerializer.SERIALIZER.deserialize(string)));
        } else {
            String var10002 = String.valueOf(obj);
            throw new SerializationException(var10002 + "(" + type + ") is not a duration or 'disabled'");
        }
    }

    protected @NonNull Object serialize(DurationOrDisabled item, @NonNull Predicate<Class<?>> typeSupported) {
        return item.value().map(Duration::value).orElse("disabled");
    }
}
