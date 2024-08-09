package net.sabafly.mailbox.type;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PDCLocalDateTimeType implements PersistentDataType<String, LocalDateTime> {

    @Override
    public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public @NotNull Class<LocalDateTime> getComplexType() {
        return LocalDateTime.class;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull LocalDateTime complex, @NotNull PersistentDataAdapterContext context) {
        return complex.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public @NotNull LocalDateTime fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        return LocalDateTime.parse(primitive, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
