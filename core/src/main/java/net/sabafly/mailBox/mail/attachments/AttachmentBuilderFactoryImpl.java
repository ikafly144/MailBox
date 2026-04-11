package net.sabafly.mailBox.mail.attachments;


import net.kyori.adventure.text.Component;
import net.sabafly.mailbox.api.mail.attachments.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class AttachmentBuilderFactoryImpl implements AttachmentBuilderFactory {
    @Override
    public Attachment.Builder<?, ItemContent> item(@NonNull ItemStack item) {
        return ItemAttachment.ItemAttachmentBuilder.builder(item);
    }

    @Override
    public Attachment.Builder<?, CommandContent> command(@NotNull String command) {
        return CommandAttachment.CommandAttachmentBuilder.builder(command);
    }

    @Override
    public Attachment.Builder<?, MessageContent> message(@NotNull Component message) {
        return MessageAttachment.MessageAttachmentBuilder.builder(message);
    }

    @Override
    public Attachment.Builder<?, VaultValueContent> vaultValue(double value) {
        return VaultValueAttachment.VaultValueAttachmentBuilder.builder(value);
    }

}
