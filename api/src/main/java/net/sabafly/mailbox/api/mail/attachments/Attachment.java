package net.sabafly.mailbox.api.mail.attachments;

import net.sabafly.mailbox.api.mail.MailBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Attachment<C extends AttachmentContent<?>> {

    @NotNull String name();

    @NotNull ItemStack icon();

    @NotNull C content();

    interface Builder<B extends Builder<B, C>, C extends AttachmentContent<?>> extends MailBuilder<Attachment<C>> {

        Builder<B, C> content(@NotNull C content);

        Attachment<C> build();

    }

}
