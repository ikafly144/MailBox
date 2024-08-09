package net.sabafly.mailbox.type;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PDCMailActionType implements PersistentDataType<String, MailAction> {
    @Override
    public @NotNull Class<String> getPrimitiveType() {
        return String.class;
    }

    @Override
    public @NotNull Class<MailAction> getComplexType() {
        return MailAction.class;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull MailAction complex, @NotNull PersistentDataAdapterContext context) {
        return MailAction.serialize(complex);
    }

    @Override
    public @NotNull MailAction fromPrimitive(@NotNull String primitive, @NotNull PersistentDataAdapterContext context) {
        return MailAction.deserialize(primitive);
    }

}
