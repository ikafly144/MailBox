package net.sabafly.mailbox.library;

import net.sabafly.mailbox.v1_21_R0_1.NMSImplV1_21_1;
import org.bukkit.Bukkit;

public class LibNMSAccessor {

    private static LibNMS instance = get();

    public static LibNMS get() {
        if (instance != null)
            return instance;
        var version = Bukkit.getServer().getBukkitVersion();
        var nms = switch (version) {
            case "1.21-R0.1-SNAPSHOT" -> new NMSImplV1_21_1();
            default -> throw new IllegalStateException("Unsupported Paper version: " + version);
        };
        instance = nms;
        return nms;
    }
}
