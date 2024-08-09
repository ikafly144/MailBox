package net.sabafly.mailbox.type;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PDCMailBoxEntryType implements PersistentDataType<PersistentDataContainer, MailBoxEntry> {
    @Override
    public @NotNull Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    public @NotNull Class<MailBoxEntry> getComplexType() {
        return MailBoxEntry.class;
    }

    @Override
    public @NotNull PersistentDataContainer toPrimitive(@NotNull MailBoxEntry complex, @NotNull PersistentDataAdapterContext context) {
        var container = context.newPersistentDataContainer();
        container.set(createKey("item"), new PDCConfigurationSerializableType<>(ItemStack.class), complex.getItemStack());
        container.set(createKey("name"), new PDCComponentType(), complex.getName());
        container.set(createKey("description"), new PDCComponentType(), complex.getDescription());
        container.set(createKey("action"), new PDCMailActionType(), complex.getAction());
        if (complex.getReceived() != null)
            container.set(createKey("received"), new PDCLocalDateTimeType(), complex.getReceived());
        return container;
    }

    @Override
    public @NotNull MailBoxEntry fromPrimitive(@NotNull PersistentDataContainer primitive, @NotNull PersistentDataAdapterContext context) {
        var itemStack = primitive.getOrDefault(createKey("item"), new PDCConfigurationSerializableType<>(ItemStack.class), ItemStack.of(Material.STONE));
        var name = primitive.getOrDefault(createKey("name"), new PDCComponentType(), Component.text("[NO DATA]"));
        var description = primitive.getOrDefault(createKey("description"), new PDCComponentType(), Component.text("[NO DATA]"));
        var action = primitive.getOrDefault(createKey("action"), new PDCMailActionType(), MailAction.NONE);
        var received = primitive.get(createKey("received"), new PDCLocalDateTimeType());
        return MailBoxEntry.create(itemStack, name, description, action, received);
    }

    private static @NotNull NamespacedKey createKey(String key) {
        return new NamespacedKey("mailbox", key);
    }

}
