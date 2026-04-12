package net.sabafly.mailBox.utils;

import net.sabafly.mailBox.MailBox;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ThreadUtils {

    private ThreadUtils() {
    }

    public static void runSync(@NotNull Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute(MailBox.getInstance(), runnable);
    }

    public static void runSync(@NotNull Player player, @NotNull Runnable runnable) {
        player.getScheduler().run(MailBox.getInstance(), _ -> runnable.run(), null);
    }

}
