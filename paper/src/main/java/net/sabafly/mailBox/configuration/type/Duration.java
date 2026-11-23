package net.sabafly.mailBox.configuration.type;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.serialize.ScalarSerializer;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Duration {
    private static final Pattern SPACE = Pattern.compile(" ");
    private static final Pattern NOT_NUMERIC = Pattern.compile("[^-\\d.]");
    public static final ScalarSerializer<Duration> SERIALIZER = new Serializer();
    private final long seconds;
    private final String value;

    private Duration(String value) {
        this.value = value;
        this.seconds = getSeconds(value);
    }

    public long seconds() {
        return this.seconds;
    }

    public long ticks() {
        return this.seconds * 20L;
    }

    public String value() {
        return this.value;
    }

    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Duration duration = (Duration) o;
            return this.seconds == duration.seconds && this.value.equals(duration.value);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(this.seconds, this.value);
    }

    public String toString() {
        return "Duration{seconds=" + this.seconds + ", value='" + this.value + "'}";
    }

    public static Duration of(String time) {
        return new Duration(time);
    }

    private static int getSeconds(String str) {
        str = SPACE.matcher(str).replaceAll("");
        char unit = str.charAt(str.length() - 1);
        str = NOT_NUMERIC.matcher(str).replaceAll("");

        double num;
        try {
            num = Double.parseDouble(str);
        } catch (Exception var5) {
            num = 0.0F;
        }

        switch (unit) {
            case 'd':
                num *= 86400.0F;
                break;
            case 'h':
                num *= 3600.0F;
                break;
            case 'm':
                num *= 60.0F;
            case 's':
        }

        return (int) num;
    }

    static final class Serializer extends ScalarSerializer<Duration> {
        private Serializer() {
            super(Duration.class);
        }

        public Duration deserialize(@NonNull Type type, Object obj) {
            return new Duration(obj.toString());
        }

        protected @NonNull Object serialize(Duration item, @NonNull Predicate<Class<?>> typeSupported) {
            return item.value();
        }
    }
}
