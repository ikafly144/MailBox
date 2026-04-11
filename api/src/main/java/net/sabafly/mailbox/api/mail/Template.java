package net.sabafly.mailbox.api.mail;

import net.sabafly.mailbox.api.mail.attachments.Attachment;
import net.sabafly.mailbox.api.mail.attachments.AttachmentBuilderFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public interface Template {

    @NotNull String subject();

    @NotNull String content();

    @NotNull User sender();

    void send(User target);

    interface Builder<B extends Builder<?>> extends Template, MailBuilder<Template> {

        B subject(String subject);

        B content(String content);

        B sender(User sender);

        B attachments(@NotNull Function<AttachmentBuilderFactory, @NotNull List<Attachment<?>>> attachments);

        Template build();

    }

}
