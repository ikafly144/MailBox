package net.sabafly.mailbox.api.mail.attachments;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Attachment {

    @NotNull String name();

    @NotNull ItemStack icon();

}
