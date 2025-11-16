package net.sabafly.mailbox.api.mail.attachments;

import java.util.UUID;

public interface MailAttachment extends Attachment {

    UUID id();

    boolean isRead();

    void open();

}
