package net.sabafly.mailBox.mail;

import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PlayerMailUser(@NotNull OfflinePlayer offlinePlayer) implements User {

    @Override
    public @NotNull String toString() {
        return offlinePlayer.toString();
    }

    @Override
    public @NotNull UUID id() {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public @NotNull String name() {
        // TODO: 名前をデータベースに保存する
        return Objects.requireNonNullElse(offlinePlayer.getName(), offlinePlayer.getUniqueId().toString());
    }

    @NotNull
    public OfflinePlayer offlinePlayer() {
        return offlinePlayer;
    }

}
