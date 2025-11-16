package net.sabafly.mailBox.mail;

import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record MailUser(@NotNull UUID uuid) implements User {

    @Override
    public @NotNull String toString() {
        return uuid.toString();
    }

    @Override
    public @NotNull UUID id() {
        return uuid;
    }

    @Override
    public @NotNull String name() {
        // TODO: 名前をデータベースに保存する
        return Objects.requireNonNull(Bukkit.getOfflinePlayer(uuid).getName());
    }
}
