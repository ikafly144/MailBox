package net.sabafly.mailBox.mail.attachments;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class CommandAttachment extends BaseAttachment<CommandAttachment> {

    private final String command;

    public CommandAttachment(@NotNull String name, @NotNull String command, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        super(name, Type.COMMAND, received, itemType, expireTime);
        this.command = command;
    }

    protected CommandAttachment(UUID uuid, @NotNull String name, @NotNull String command, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        super(uuid, name, Type.COMMAND, received, itemType, expireTime);
        this.command = command;
    }

    @Override
    protected @NotNull ItemType defaultPreviewType() {
        return ItemType.COMMAND_BLOCK;
    }

    @Override
    public void apply(@NotNull Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + player.getName() + " at @s run " + command);
    }

    @Override
    public void cancel(@NotNull Player player) {
    }

    @Override
    public byte @NotNull [] serialize() {
        return command.getBytes();
    }

    public static @NotNull CommandAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        return new CommandAttachment(id, name, new String(data), received, itemType, expireTime);
    }

    @Override
    public @NotNull CommandAttachment create(boolean received) {
        return new CommandAttachment(getName(), command, received, getPreviewType(), getExpireTime().orElse(null));
    }
}
