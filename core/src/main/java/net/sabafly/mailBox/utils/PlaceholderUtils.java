package net.sabafly.mailBox.utils;

import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.MailBox;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderUtils {

    public static ComponentLike deserialize(@NotNull OfflinePlayer player, @NotNull String string, @NotNull TagResolver... resolvers) {
        if (MailBox.isPlaceholderApiEnabled()) {
            string = setPlaceholder(player, string);
        }
        return MiniMessage.miniMessage().deserialize(string, resolvers);
    }

    public static @NotNull String setPlaceholder(OfflinePlayer player, @NotNull String message) {
        if (!MailBox.isPlaceholderApiEnabled()) {
            return message;
        }
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, message);
    }

}
