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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public interface Attachment<T extends Attachment<T>> extends Cloneable {

    @NotNull
    UUID getId();

    @NotNull
    Type getType();

    @NotNull
    Component getName();

    ItemStack DEFAULT_ITEM = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.BUNDLE).createItemStack();

    @NotNull
    default ItemStack getPreview() {
        return getPreview(null);
    }

    @NotNull
    default ItemStack getPreview(@Nullable Function<@NotNull Attachment<T>,@NotNull List<@NotNull Component>> loreSupplier) {
        ItemStack item = getPreviewType() != null ? getPreviewType().createItemStack() : DEFAULT_ITEM.clone();
        item.editMeta(meta -> {
            meta.itemName(getName());
            if (loreSupplier != null) {
                meta.lore(loreSupplier.apply(this));
            }
            if (meta instanceof BundleMeta bundleMeta) bundleMeta.setItems(null);
        });
        return item;
    }

    @Nullable
    ItemType getPreviewType();

    void apply(@NotNull Player player);

    void cancel(@NotNull Player player);

    boolean opened();

    void setOpened(boolean opened);

    Optional<LocalDateTime> getReceivedTime();

    boolean isTemplate();

    boolean isExpired();

    @NotNull
    Optional<Duration> expireDuration();

    void expireDuration(@Nullable Duration expireDuration);

    byte @NotNull [] serialize();

    static Attachment<?> deserialize(@NotNull Type type, @NotNull UUID uuid, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireTime) {
        return switch (type) {
            case ITEM -> ItemAttachment.deserialize(uuid, name, received, data, itemType, receivedTime, expireTime);
            case VAULT_VALUE -> VaultValueAttachment.deserialize(uuid, name, received, data, itemType, receivedTime, expireTime);
            case COMMAND -> CommandAttachment.deserialize(uuid, name, received, data, itemType, receivedTime, expireTime);
            case MESSAGE -> MessageAttachment.deserialize(uuid, name, received, data, itemType, receivedTime, expireTime);
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
