package net.sabafly.mailbox.api.mail.attachments;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface AttachmentBuilderFactory {

    Attachment.Builder<?, ItemContent> item(@NotNull ItemStack item);

    Attachment.Builder<?, CommandContent> command(@NotNull String command);

    Attachment.Builder<?, MessageContent> message(@NotNull Component message);

    Attachment.Builder<?, VaultValueContent> vaultValue(double value);

}
