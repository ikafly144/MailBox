package net.sabafly.mailbox.type;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PDCComponentType implements PersistentDataType<String, Component> {

    @Override
    public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public @NotNull Class<Component> getComplexType() {
        return Component.class;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull Component complex, @NotNull PersistentDataAdapterContext context) {
        return JSONComponentSerializer.json().serialize(complex);
    }

    @Override
    public @NotNull Component fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        return JSONComponentSerializer.json().deserialize(primitive);
    }
}
