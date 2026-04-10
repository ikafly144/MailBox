package net.sabafly.mailBox.mail.attachments;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class MessageAttachment extends BaseAttachment<MessageAttachment> {

    private final String message;

    public MessageAttachment(@NotNull String name, boolean received, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String message) {
        super(name, Type.MESSAGE, received, previewItem, receivedTime, expireDuration);
        this.message = message;
    }

    public MessageAttachment(@NotNull UUID id, @NotNull String name, boolean received, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String message) {
        super(id, name, Type.MESSAGE, received, previewItem, receivedTime, expireDuration);
        this.message = message;
    }

    @Override
    public boolean opened() {
        return false;
    }

    @Override
    public void apply(@NotNull Player player) {
        player.sendMessage(miniMessage().deserialize(message));
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
        return message.getBytes();
    }

    public static @NotNull MessageAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new MessageAttachment(id, name, received, previewItem, receivedTime, expireDuration, new String(data));
    }

    @Override
    public @NotNull MessageAttachment create(boolean opened) {
        return new MessageAttachment(miniMessage().serialize(getName()), opened, getPreviewItem(), LocalDateTime.now(), expireDuration().orElse(config().mail.getExpirationDuration()), message);
    }
}
