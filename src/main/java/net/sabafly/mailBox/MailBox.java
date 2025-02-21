package net.sabafly.mailBox;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import net.sabafly.mailBox.commands.MailCommands;
import net.sabafly.mailBox.configuration.Config;
import net.sabafly.mailBox.configuration.ConfigLoader;
import net.sabafly.mailBox.database.Database;
import net.sabafly.mailBox.executor.ThreadedQueue;
import net.sabafly.mailBox.listener.PlayerListener;
import net.sabafly.mailBox.menu.MenuManager;
import net.sabafly.mailBox.schedule.ScheduleManager;
import net.sabafly.mailBox.utils.EconomyUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public final class MailBox extends JavaPlugin implements Listener {

    private ConfigLoader config;
    private Database database;
    @Getter
    private static ThreadedQueue<Runnable> threadedQueue;
    private static Logger logger;
    private MenuManager menuManager;
    private ScheduleManager scheduleManager;
    private boolean vaultEnabled;

    public static Logger logger() {
        return logger;
    }

    @Override
    public void onLoad() {
        logger = getSLF4JLogger();
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        menuManager = new MenuManager(this);
        PacketEvents.getAPI().getEventManager().registerListener(menuManager, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();
        threadedQueue = new ThreadedQueue<>("MailBox-Worker-Thread");

        config = new ConfigLoader(getDataPath());
        config.reload();

        this.database = config.config().database.loadDatabase();
        this.database.setup();

        menuManager.register();

        scheduleManager = new ScheduleManager(this);
        scheduleManager.start();

        new PlayerListener().register(this);

        new MailCommands(this).registerCommands();

        Bukkit.getScheduler().runTask(this, this::loadVault);
    }

    @Override
    public void onDisable() {
        scheduleManager.stop();
        this.database.close();
        PacketEvents.getAPI().terminate();
        threadedQueue.stop();
    }

    public static MailBox getInstance() {
        return getPlugin(MailBox.class);
    }

    public static Config config() {
        return getInstance().config.config();
    }

    public static Database database() {
        return getInstance().database;
    }

    public static void reload() {
        getInstance().config.reload();
    }

    public static boolean isVaultEnabled() {
        return getInstance().vaultEnabled;
    }

    private void loadVault() {
        if (getInstance().getServer().getPluginManager().getPlugin("Vault") != null) {
            logger().info("Vault found! Enabling support for it.");
            try {
                EconomyUtils.getEconomy();
            } catch (Exception e) {
                logger().error("Failed to load Vault economy", e);
                logger().warn("May be caused by missing economy plugin or incorrect configuration.");
                return;
            }
            vaultEnabled = true;
        }
    }

}
