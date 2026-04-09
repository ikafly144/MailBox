package net.sabafly.mailBox.mail;

import com.google.common.base.Preconditions;
import net.kyori.adventure.key.Key;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PlayerMailUser(@NotNull OfflinePlayer player, @NotNull Key key) implements User {

    public PlayerMailUser {
        Preconditions.checkArgument(player.getName() != null, "OfflinePlayer must have a name to generate a key");
        Preconditions.checkArgument(player.hasPlayedBefore() || player.isOnline(), "OfflinePlayer must have played before or be online to generate a key");
        Preconditions.checkArgument(key.namespace().equals("minecraft"), "Key namespace must be 'minecraft' for PlayerMailUser");
    }

    @SuppressWarnings("PatternValidation")
    public static @NotNull Key keyOf(@NotNull OfflinePlayer offlinePlayer) {
        var name = offlinePlayer.getName();
        Preconditions.checkArgument(name != null, "OfflinePlayer must have a name to generate a key");
        return Key.key("minecraft", name);
    }

    @Override
    public @NotNull String toString() {
        return player.toString();
    }

    @Override
    public @NotNull UUID id() {
        return player.getUniqueId();
    }

    @Override
    public @NotNull String name() {
        return Objects.requireNonNullElse(player.getName(), player.getUniqueId().toString());
    }

    @NotNull
    public OfflinePlayer player() {
        return player;
    }

}
