package net.sabafly.mailBox.mail;

import net.kyori.adventure.key.Key;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PlayerMailUser(@NotNull OfflinePlayer offlinePlayer, @NotNull Key key) implements User {

    @SuppressWarnings("PatternValidation")
    public static @NotNull Key keyOf(@NotNull OfflinePlayer offlinePlayer) {
        return Key.key("minecraft", offlinePlayer.getUniqueId().toString());
    }

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
        return Objects.requireNonNullElse(offlinePlayer.getName(), offlinePlayer.getUniqueId().toString());
    }

    @NotNull
    public OfflinePlayer offlinePlayer() {
        return offlinePlayer;
    }

}
