package net.sabafly.mailBox.mail;

import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.mail.attachments.CommandAttachment;
import net.sabafly.mailBox.mail.attachments.ItemAttachment;
import net.sabafly.mailBox.mail.attachments.MessageAttachment;
import net.sabafly.mailBox.mail.attachments.VaultValueAttachment;
import net.sabafly.mailbox.api.mail.attachments.MailAttachment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface Attachment<T extends Attachment<T>> extends Cloneable, MailAttachment {

    @NotNull
    UUID getId();

    @NotNull
    Type getType();

    @NotNull
    Component getName();

    @NotNull
    String getPlainName();

    @NotNull
    default ItemStack createPreview(@Nullable Function<@NotNull Attachment<T>,@NotNull List<@NotNull Component>> loreSupplier) {
        ItemStack item = getPreviewItem();
        item.editMeta(meta -> {
            meta.itemName(getName());
            if (loreSupplier != null) {
                meta.lore(loreSupplier.apply(this));
            }
            if (meta instanceof BundleMeta bundleMeta) bundleMeta.setItems(null);
        });
        return item;
    }

    @NotNull
    ItemStack getPreviewItem();

    void apply(@NotNull Player player);

    void cancel(@NotNull Player player);

    boolean opened();

    void setOpened(boolean opened);

    @Nullable
    LocalDateTime getReceivedTime();

    void setReceivedTime(@NotNull LocalDateTime receivedTime);

    boolean isTemplate();

    boolean isExpired();

    @NotNull
    Optional<Duration> expireDuration();

    default Optional<LocalDateTime> expireTime() {
        if (getReceivedTime() == null || expireDuration().isEmpty() || isTemplate()) {
            return Optional.empty();
        }
        return expireDuration().map(duration -> getReceivedTime().plus(duration));
    }

    void expireDuration(@Nullable Duration expireDuration);

    byte @NotNull [] serialize();

    static Attachment<?> deserialize(@NotNull Type type, @NotNull UUID uuid, @NotNull String name, boolean received, byte @NotNull [] data, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireTime) {
        return switch (type) {
            case ITEM -> ItemAttachment.deserialize(uuid, name, received, data, previewItem, receivedTime, expireTime);
            case VAULT_VALUE -> VaultValueAttachment.deserialize(uuid, name, received, data, previewItem, receivedTime, expireTime);
            case COMMAND -> CommandAttachment.deserialize(uuid, name, received, data, previewItem, receivedTime, expireTime);
            case MESSAGE -> MessageAttachment.deserialize(uuid, name, received, data, previewItem, receivedTime, expireTime);
        };
    }

    @NotNull
    T create(boolean opened);

    @NotNull
    default T create() {
        return create(false);
    }

    @Override
    default UUID id() {
        return getId();
    }

    @Override
    default boolean isRead() {
        return opened();
    }

    @Override
    default void open() {
        setOpened(true);
    }

    @Override
    default @NotNull String name() {
        return getPlainName();
    }

    @Override
    default @NotNull ItemStack icon() {
        return getPreviewItem();
    }

    enum Type {
        ITEM,
        VAULT_VALUE,
        COMMAND,
        MESSAGE
    }

}
