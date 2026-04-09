package net.sabafly.mailbox.api.mail;

import org.jetbrains.annotations.NotNull;

public interface Template {

    @NotNull String subject();

    @NotNull String content();

    @NotNull User sender();

    void send(User target);

    interface Builder<B extends Builder<?>> extends Template, MailBuilder<Template> {

        B subject(String subject);

        B content(String content);

        B sender(User sender);

        Template build();

    }

}
