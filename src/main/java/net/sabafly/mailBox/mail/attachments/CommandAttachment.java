package net.sabafly.mailBox.mail.attachments;

import org.bukkit.Bukkit;
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
public class CommandAttachment extends BaseAttachment<CommandAttachment> {

    private final String command;

    public CommandAttachment(@NotNull String name, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String command) {
        super(name, Type.COMMAND, received, itemType, receivedTime, expireDuration);
        this.command = command;
    }

    public CommandAttachment(@NotNull UUID id, @NotNull String name, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String command) {
        super(id, name, Type.COMMAND, received, itemType, receivedTime, expireDuration);
        this.command = command;
    }

    @Override
    protected @NotNull ItemType defaultPreviewType() {
        return ItemType.COMMAND_BLOCK;
    }

    @Override
    public void apply(@NotNull Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()));
    }

    @Override
    public void cancel(@NotNull Player player) {
    }

    @Override
    public byte @NotNull [] serialize() {
        return command.getBytes();
    }

    public static @NotNull CommandAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new CommandAttachment(id, name, received, itemType, receivedTime, expireDuration, new String(data));
    }

    @Override
    public @NotNull CommandAttachment create(boolean received) {
        return new CommandAttachment(miniMessage().serialize(getName()), received, getPreviewType(), null, config().mail.getExpirationTime(), command);
    }
}
