package net.sabafly.mailbox.api.mail;

import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.key.Namespaced;
import net.sabafly.mailbox.api.exception.MailException;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public interface PluginUser extends User, Keyed, Namespaced {

    int PAGE_SIZE = 27;

    @NotNull Plugin plugin();

    @NotNull Template createTemplate(Consumer<Template.Builder<?>> builderConsumer);

    @NotNull List<Template> templates(int page);

    @NotNull Template getTemplate(@NotNull String name) throws MailException;

    @NotNull List<Mail> mails(int page);

}
