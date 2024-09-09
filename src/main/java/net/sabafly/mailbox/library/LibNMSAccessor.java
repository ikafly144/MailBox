package net.sabafly.mailbox.library;

import net.sabafly.mailbox.v1_21_R0_1.NMSImplV1_21_1;
import org.bukkit.Bukkit;

public class LibNMSAccessor {

    private static LibNMS instance = get();

    public static LibNMS get() {
        if (instance != null)
            return instance;
        var version = Bukkit.getServer().getMinecraftVersion();
        var nms = switch (version) {
            case "1.21", "1.21.1" -> new NMSImplV1_21_1();
            default -> throw new IllegalStateException("Unsupported Paper version: " + version);
        };
        instance = nms;
        return nms;
    }
}
