package net.sabafly.mailbox.api.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailbox.api.mail.MailBuilder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface Attachment<C extends AttachmentContent<?>> {

    @NotNull String name();

    @NotNull Component format();

    @NotNull ItemStack icon();

    @Nullable Duration expireDuration();

    @NotNull C content();

    interface Builder<B extends Builder<B, C>, C extends AttachmentContent<?>> extends MailBuilder<Attachment<C>> {

        B name(@NotNull String name);

        B icon(@NotNull ItemStack icon);

        B expireDuration(@Nullable Duration expireDuration);

        Attachment<C> build();

    }

}
