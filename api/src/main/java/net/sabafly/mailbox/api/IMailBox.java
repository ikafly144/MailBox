package net.sabafly.mailbox.api;

import net.sabafly.mailbox.api.mail.Mail;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IMailBox {

    static @Nullable IMailBox getInstance() {
        return (IMailBox) Bukkit.getServer().getPluginManager().getPlugin("MailBox");
    }

    @Nullable Mail createMail(@NotNull String subject, @NotNull String content, @Nullable User sender, @NotNull User receiver);

    @Nullable User getUser(@NotNull Player player);

}
