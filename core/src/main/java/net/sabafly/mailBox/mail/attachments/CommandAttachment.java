package net.sabafly.mailBox.mail.attachments;

import net.sabafly.mailbox.api.mail.attachments.CommandContent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class CommandAttachment extends BaseAttachment<CommandAttachment, CommandContent> {

    private final CommandContent command;

    public CommandAttachment(@NotNull String name, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String command) {
        super(name, Type.COMMAND, opened, previewItem, receivedTime, expireDuration);
        this.command = CommandContent.of(command);
    }

    public CommandAttachment(@NotNull UUID id, @NotNull String name, boolean received, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, String command) {
        super(id, name, Type.COMMAND, received, previewItem, receivedTime, expireDuration);
        this.command = CommandContent.of(command);
    }

    private CommandAttachment(@NotNull BaseAttachment<?, CommandContent> base) {
        super(base);
        this.command = base.content();
    }

    @Override
    public void apply(@NotNull Player player) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), content().value().replaceAll("\\{player}", Matcher.quoteReplacement(player.getName())));
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
        return content().value().getBytes();
    }

    public static @NotNull CommandAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new CommandAttachment(id, name, received, previewItem, receivedTime, expireDuration, new String(data));
    }

    @Override
    public @NotNull CommandAttachment create(boolean opened) {
        return new CommandAttachment(miniMessage().serialize(getName()), opened, getPreviewItem(), LocalDateTime.now(), expireDuration().orElse(config().mail.getExpirationDuration()), content().value());
    }

    @Override
    public @NonNull CommandContent content() {
        return command;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static final class CommandAttachmentBuilder extends BaseBuilder<CommandAttachment, CommandAttachmentBuilder, CommandContent> {

        public static CommandAttachmentBuilder builder(@NotNull String command) {
            return new CommandAttachmentBuilder(UUID.randomUUID(), "COMMAND", false, ItemType.COMMAND_BLOCK.createItemStack(), null, null, CommandContent.of(command));
        }

        private CommandAttachmentBuilder(@NotNull UUID id, @NotNull String name, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, CommandContent content) {
            super(id, name, Type.COMMAND, opened, previewItem, receivedTime, expireDuration, content);
        }

        @Override
        public @NonNull CommandAttachment create(boolean opened) {
            return new CommandAttachment(this);
        }
    }

}
