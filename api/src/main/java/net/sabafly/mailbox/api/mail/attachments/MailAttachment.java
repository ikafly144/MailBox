package net.sabafly.mailbox.api.mail.attachments;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface MailAttachment<C extends AttachmentContent<?>> extends Attachment<C> {

    UUID id();

    boolean isRead();

    void open(@NotNull Player player);

}
