package net.sabafly.mailBox.mail.attachments;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class CommandAttachment extends BaseAttachment<CommandAttachment> {

    private final String command;

    public CommandAttachment(@NotNull String name, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String command) {
        super(name, Type.COMMAND, opened, previewItem, receivedTime, expireDuration);
        this.command = command;
    }

    public CommandAttachment(@NotNull UUID id, @NotNull String name, boolean received, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String command) {
        super(id, name, Type.COMMAND, received, previewItem, receivedTime, expireDuration);
        this.command = command;
    }

    @Override
    public void apply(@NotNull Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{player}", Matcher.quoteReplacement(player.getName())));
    }

    @Override
    public void cancel(@NotNull Player player) {
    }

    @Override
    public byte @NotNull [] serialize() {
        return command.getBytes();
    }

    public static @NotNull CommandAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new CommandAttachment(id, name, received, previewItem, receivedTime, expireDuration, new String(data));
    }

    @Override
    public @NotNull CommandAttachment create(boolean opened) {
        return new CommandAttachment(miniMessage().serialize(getName()), opened, getPreviewItem(), LocalDateTime.now(), expireDuration().orElse(config().mail.getExpirationDuration()), command);
    }
}
