package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailbox.api.mail.attachments.MessageContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class MessageAttachment extends BaseAttachment<MessageAttachment, MessageContent> {

    private final MessageContent message;

    public MessageAttachment(@NotNull String name, boolean received, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, Component message) {
        super(name, Type.MESSAGE, received, previewItem, receivedTime, expireDuration);
        this.message = MessageContent.of(message);
    }

    public MessageAttachment(@NotNull UUID id, @NotNull String name, boolean received, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, Component message) {
        super(id, name, Type.MESSAGE, received, previewItem, receivedTime, expireDuration);
        this.message = MessageContent.of(message);
    }

    public MessageAttachment(@NotNull BaseAttachment<?, MessageContent> base) {
        super(base);
        this.message = base.content();
    }

    @Override
    public boolean opened() {
        return false;
    }

    @Override
    public void apply(@NotNull Player player) {
        player.sendMessage(message.value());
    }

    @Override
    public boolean checkRequirement(@NotNull Player player) {
        return true;
    }

    @Override
    public boolean consumeRequirement(@NotNull Player player) {
        return true;
    }

    @Override
    public byte @NotNull [] serialize() {
        return miniMessage().serialize(content().value()).getBytes();
    }

    public static @NotNull MessageAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new MessageAttachment(id, name, received, previewItem, receivedTime, expireDuration, miniMessage().deserialize(new String(data)));
    }

    @Override
    public @NotNull MessageAttachment create(boolean opened) {
        return new MessageAttachment(miniMessage().serialize(getName()), opened, getPreviewItem(), LocalDateTime.now(), expireDuration().orElse(config().mail.getExpirationDuration()), content().value());
    }

    @Override
    public @NonNull MessageContent content() {
        return message;
    }

    @Override
    public boolean canOpen() {
        return !isExpired();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final class MessageAttachmentBuilder extends BaseBuilder<MessageAttachment, MessageAttachmentBuilder, MessageContent> {

        public static MessageAttachmentBuilder builder(@NotNull Component message) {
            return new MessageAttachmentBuilder(UUID.randomUUID(), "MESSAGE", false, ItemType.BOOK.createItemStack(), null, null, MessageContent.of(message));
        }

        private MessageAttachmentBuilder(@NotNull UUID id, @NotNull String name, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, MessageContent content) {
            super(id, name, Type.MESSAGE, opened, previewItem, receivedTime, expireDuration, content);
        }

        @Override
        public @NonNull MessageAttachment create(boolean opened) {
            return new MessageAttachment(this);
        }
    }

}
