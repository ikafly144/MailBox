package net.sabafly.mailBox.mail;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailbox.api.exception.MailException;
import net.sabafly.mailbox.api.mail.Mail;
import net.sabafly.mailbox.api.mail.PluginUser;
import net.sabafly.mailbox.api.mail.Template;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.sabafly.mailBox.MailBox.database;

public final class PluginMailUser extends DummyMailUser implements PluginUser {

    @SuppressWarnings("PatternValidation")
    @ApiStatus.Internal
    public static @NotNull PluginMailUser createPlugin(@NotNull UUID uuid, @NotNull String name, @NotNull Plugin plugin) {
        return new PluginMailUser(uuid, name, plugin, User.sanitizeName(name));
    }

    @SuppressWarnings("PatternValidation")
    public static @NotNull PluginMailUser createPlugin(@NotNull String name, @NotNull Plugin plugin) {
        return new PluginMailUser(UUID.randomUUID(), name, plugin, User.sanitizeName(name));
    }

    private final Plugin plugin;

    private PluginMailUser(@NotNull UUID uuid, @NotNull String name, @NotNull Plugin plugin, @NotNull @KeyPattern.Value String keyValue) {
        super(uuid, name, Key.key(plugin, keyValue));
        this.plugin = plugin;
    }

    @Override
    public @NotNull Plugin plugin() {
        return plugin;
    }

    @Override
    public @NotNull Template createTemplate(Consumer<Template.Builder<?>> builderConsumer) {
        var builder = new MailTemplate.PluginTemplateBuilder(
                java.util.UUID.randomUUID(),
                "No Title",
                "No Content",
                java.util.Collections.emptyList(),
                false,
                this,
                null,
                null,
                null,
                null
        );
        builderConsumer.accept(builder);
        return builder.build();
    }

    @Override
    public @NotNull List<Template> templates(int page) {
        return database().getMailTemplatesBySender(this, page).stream().collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NotNull Template getTemplate(@NotNull String name) throws MailException {
        return database().getAllMailTemplates().stream()
                .filter(template -> template.sender().id().equals(this.id()))
                .filter(template -> template.subject().equals(name))
                .findFirst()
                .orElseThrow(() -> MailException.TEMPLATE_NOT_FOUND);
    }

    @Override
    public @NotNull List<Mail> mails(int page) {
        return database().getMails(this, TriState.NOT_SET, page).stream().collect(Collectors.toUnmodifiableList());
    }

    @KeyPattern.Namespace
    @Override
    public @NotNull String namespace() {
        //noinspection PatternValidation
        return plugin.namespace();
    }
}
