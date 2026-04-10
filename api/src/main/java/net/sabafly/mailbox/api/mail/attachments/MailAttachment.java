package net.sabafly.mailbox.api.mail.attachments;

import java.util.UUID;

public interface MailAttachment<C extends AttachmentContent<?>> extends Attachment<C> {

    UUID id();

    boolean isRead();

    void open();

}
