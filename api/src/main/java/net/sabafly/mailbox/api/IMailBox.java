package net.sabafly.mailbox.api;

import net.sabafly.mailbox.api.exception.MailException;
import net.sabafly.mailbox.api.mail.PluginUser;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IMailBox {

    static @Nullable IMailBox getInstance() {
        return (IMailBox) Bukkit.getServer().getPluginManager().getPlugin("MailBox");
    }

    @NotNull User getUser(@NotNull Player player) throws MailException;

    @NotNull PluginUser registerPluginUser(@NotNull Plugin plugin, @NotNull String name) throws MailException;

    @NotNull PluginUser getPluginUser(@NotNull Plugin plugin, @NotNull String name) throws MailException;

}
