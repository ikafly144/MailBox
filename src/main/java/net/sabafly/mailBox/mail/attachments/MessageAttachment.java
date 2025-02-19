package net.sabafly.mailBox.mail.attachments;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@SuppressWarnings("UnstableApiUsage")
public class MessageAttachment extends BaseAttachment<MessageAttachment> {

    private final String message;

    public MessageAttachment(@NotNull String name, @NotNull String message, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        super(name, Type.MESSAGE, received, itemType, expireTime);
        this.message = message;
    }

    protected MessageAttachment(@NotNull UUID id, @NotNull String name, @NotNull String message, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        super(id, name, Type.MESSAGE, received, itemType, expireTime);
        this.message = message;
    }

    @Override
    public boolean received() {
        return false;
    }

    @Override
    protected @NotNull ItemType defaultPreviewType() {
        return ItemType.WRITTEN_BOOK;
    }

    @Override
    public void apply(@NotNull Player player) {
        player.sendMessage(miniMessage().deserialize(message));
    }

    @Override
    public byte @NotNull [] serialize() {
        return message.getBytes();
    }

    public static @NotNull MessageAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        return new MessageAttachment(id, name, new String(data), received, itemType, expireTime);
    }

    @Override
    public @NotNull MessageAttachment create(boolean received) {
        return new MessageAttachment(getName(), message, received, getPreviewType(), getExpireTime().orElse(null));
    }
}
