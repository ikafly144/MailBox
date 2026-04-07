package net.sabafly.mailbox.api.mail;

import org.jetbrains.annotations.NotNull;

public interface Template {

    @NotNull String subject();

    @NotNull String content();

    @NotNull User sender();

    interface Builder extends Template, MailBuilder<Template> {

        Builder subject(String subject);

        Builder content(String content);

        Builder sender(User sender);

        Template build();

    }

}
