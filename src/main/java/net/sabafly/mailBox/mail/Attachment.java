package net.sabafly.mailBox.mail;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.mail.attachments.CommandAttachment;
import net.sabafly.mailBox.mail.attachments.ItemAttachment;
import net.sabafly.mailBox.mail.attachments.MessageAttachment;
import net.sabafly.mailBox.mail.attachments.VaultValueAttachment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.BundleMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public interface Attachment<T extends Attachment<T>> extends Cloneable {

    @NotNull
    UUID getId();

    @NotNull
    Type getType();

    @NotNull
    String getName();

    ItemStack DEFAULT_ITEM = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.BUNDLE).createItemStack();

    @NotNull
    default ItemStack getPreview() {
        ItemStack item = getPreviewType() != null ? getPreviewType().createItemStack() : DEFAULT_ITEM.clone();
        item.editMeta(meta -> {
            meta.itemName(Component.text(getName()));
            if (meta instanceof BundleMeta bundleMeta) bundleMeta.setItems(null);
        });
        return item;
    }

    @Nullable
    ItemType getPreviewType();

    void apply(@NotNull Player player);

    boolean received();

    void setReceived(boolean received);

    boolean isExpired();

    @NotNull
    Optional<LocalDateTime> getExpireTime();

    byte @NotNull [] serialize();

    static Attachment<?> deserialize(@NotNull Type type, @NotNull UUID uuid, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        return switch (type) {
            case ITEM -> ItemAttachment.deserialize(uuid, name, received, data, itemType, expireTime);
            case VAULT_VALUE -> VaultValueAttachment.deserialize(uuid, name, received, data, expireTime);
            case COMMAND -> CommandAttachment.deserialize(uuid, name, received, data, itemType, expireTime);
            case MESSAGE -> MessageAttachment.deserialize(uuid, name, received, data, itemType, expireTime);
        };
    }

    @NotNull
    T create(boolean received);

    @NotNull
    default T create() {
        return create(false);
    }

    enum Type {
        ITEM,
        VAULT_VALUE,
        COMMAND,
        MESSAGE
    }

}
