package net.sabafly.mailbox.api.mail;

import net.sabafly.mailbox.api.mail.attachments.MailAttachment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Mail {

    @NotNull
    String subject();

    @NotNull
    String content();

    @NotNull List<@NotNull MailAttachment> attachments();

    @NotNull User sender();

    @NotNull User receiver();

    boolean isRead();

}
