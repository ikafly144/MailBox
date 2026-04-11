package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailbox.api.mail.attachments.Attachment;
import net.sabafly.mailbox.api.mail.attachments.ItemContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class ItemAttachment extends BaseAttachment<ItemAttachment, ItemContent> {

    @NotNull
    private final ItemContent itemStack;

    public ItemAttachment(@NotNull ItemStack item, boolean received, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        super(miniMessage().serialize(getEffectiveName(item)), Type.ITEM, received, item.clone(), receivedTime, expireDuration);
        this.itemStack = ItemContent.of(item);
    }

    public ItemAttachment(@NotNull UUID id, @NotNull ItemStack item, boolean received, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        super(id, miniMessage().serialize(getEffectiveName(item)), Type.ITEM, received, item.clone(), receivedTime, expireDuration);
        this.itemStack = ItemContent.of(item);
    }

    private ItemAttachment(BaseAttachment<ItemAttachment, ItemContent> base) {
        super(base);
        this.itemStack = base.content();
    }

    private static Component getEffectiveName(@NotNull ItemStack itemStack) {
        return itemStack.effectiveName().append(itemStack.getAmount() > 1 ? Component.text(" ×" + itemStack.getAmount()) : Component.empty());
    }

    @Override
    public @NotNull Component format() {
        return format(content())
                .append(Component.text(": "))
                .append(getName());
    }

    private static Component format(ItemContent content) {
        return Component.text("ITEM[")
                .append(content.value().effectiveName())
                .append(Component.text("]"));
    }

    public @NotNull ItemContent content() {
        return itemStack;
    }

    @Override
    public String toString() {
        return "ItemAttachment{" +
                "name='" + getName() + '\'' +
                '}';
    }

    @Override
    public void apply(@NotNull Player player) {
        player.give(content().value());
    }

    @Override
    public boolean checkRequirement(@NotNull Player player) {
        return player.getInventory()
                       .all(content().value().getType())
                       .values()
                       .stream()
                       .filter(content().value()::isSimilar)
                       .mapToInt(ItemStack::getAmount)
                       .sum() >= content().value().getAmount();
    }

    @Override
    public boolean consumeRequirement(@NotNull Player player) {
        if (!checkRequirement(player)) return false;
        player.getInventory().removeItem(content().value());
        return true;
    }

    @Override
    public byte @NotNull [] serialize() {
        return content().value().serializeAsBytes();
    }

    public static @NotNull ItemAttachment deserialize(@NotNull UUID id, @NotNull String ignoredName, boolean received, byte @NotNull [] data, @Nullable ItemStack ignoredPreviewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new ItemAttachment(id, ItemStack.deserializeBytes(data), received, receivedTime, expireDuration);
    }

    @Override
    public @NotNull ItemAttachment create(boolean opened) {
        return new ItemAttachment(content().value(), false, LocalDateTime.now(), Optional.ofNullable(expireDuration()).orElse(config().mail.getExpirationDuration()));
    }

    public static final class ItemAttachmentBuilder extends BaseBuilder<ItemAttachment, ItemAttachmentBuilder, ItemContent> implements Attachment.Builder<ItemAttachmentBuilder, ItemContent> {

        public static @NotNull ItemAttachmentBuilder builder(@NotNull ItemStack item) {
            return new ItemAttachmentBuilder(UUID.randomUUID(), miniMessage().serialize(getEffectiveName(item)), false, item.clone(), null, null, ItemContent.of(item));
        }

        ItemAttachmentBuilder(@NotNull UUID id, @NotNull String name, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, ItemContent content) {
            super(id, name, Type.ITEM, opened, previewItem, receivedTime, expireDuration, content);
        }

        @Override
        public @NonNull ItemAttachment create(boolean opened) {
            return new ItemAttachment(this);
        }

        @Override
        public @NotNull Component format() {
            return ItemAttachment.format(content())
                    .append(Component.text(": "))
                    .append(getName());
        }
    }

}
