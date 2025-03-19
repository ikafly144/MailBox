package net.sabafly.mailBox.mail.attachments;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

@SuppressWarnings("UnstableApiUsage")
public class MessageAttachment extends BaseAttachment<MessageAttachment> {

    private final String message;

    public MessageAttachment(@NotNull String name, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String message) {
        super(name, Type.MESSAGE, received, itemType, receivedTime, expireDuration);
        this.message = message;
    }

    public MessageAttachment(@NotNull UUID id, @NotNull String name, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String message) {
        super(id, name, Type.MESSAGE, received, itemType, receivedTime, expireDuration);
        this.message = message;
    }

    @Override
    public boolean opened() {
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
    public void cancel(@NotNull Player player) {
    }

    @Override
    public byte @NotNull [] serialize() {
        return message.getBytes();
    }

    public static @NotNull MessageAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new MessageAttachment(id, name, received, itemType, receivedTime, expireDuration, new String(data));
    }

    @Override
    public @NotNull MessageAttachment create(boolean received) {
        return new MessageAttachment(miniMessage().serialize(getName()), received, getPreviewType(), null, config().mail.getExpirationTime(), message);
    }
}
