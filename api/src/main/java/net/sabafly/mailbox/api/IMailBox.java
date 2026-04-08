package net.sabafly.mailbox.api;

import net.sabafly.mailbox.api.exception.MailException;
import net.sabafly.mailbox.api.mail.Template;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IMailBox {

    static @Nullable IMailBox getInstance() {
        return (IMailBox) Bukkit.getServer().getPluginManager().getPlugin("MailBox");
    }

    @NotNull User getUser(@NotNull Player player) throws MailException;

    @NotNull User getSystemUser();

    @Contract("_ -> new")
    @NotNull Template createTemplate(@NotNull Consumer<Template.Builder> builderConsumer) throws MailException;

}
