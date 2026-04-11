package net.sabafly.mailBox.utils;

import net.sabafly.mailBox.MailBox;
import org.bukkit.Bukkit;

public class ThreadUtils {

    private ThreadUtils() {
    }

    public static void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(MailBox.getInstance(), runnable);
        }
    }

}
