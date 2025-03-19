package net.sabafly.mailBox;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.gson.Gson;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.ServerBuildInfo;
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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@SuppressWarnings("UnstableApiUsage")
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
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, task -> updateCheck(), 1, 60 * 60 * 20);
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

    private void updateCheck() {
        getSLF4JLogger().info("Checking for updates");
        try (var client = HttpClient.newHttpClient()) {
            var param = URLEncoder.encode("loaders=[\"paper\"]&game_versions=[\"" + ServerBuildInfo.buildInfo().minecraftVersionId() + "\"]", StandardCharsets.UTF_8);
            var uri = URI.create("https://api.modrinth.com/v2/project/S5JhwTNt/version?" + param);
            var request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(HttpResponse::body).thenAcceptAsync(buf -> {
                        var raw = new Gson().fromJson(buf, Object.class);
                        // .[0].version_number
                        var version = ((java.util.List<?>) raw).getFirst();
                        var versionNumber = ((java.util.Map<?, ?>) version).get("version_number");
                        if (!getPluginMeta().getVersion().equals(versionNumber)) {
                            getSLF4JLogger().info("A new version is available");
                            getSLF4JLogger().info("Latest version: {}", versionNumber);
                            getSLF4JLogger().info("Current version: {}", getPluginMeta().getVersion());
                        }
                    }).join();
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to check for updates: {}", e.getLocalizedMessage());
        }
    }

}
