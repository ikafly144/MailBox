package net.sabafly.mailBox;

import com.google.gson.Gson;
import com.vdurmont.semver4j.Semver;
import io.papermc.paper.ServerBuildInfo;
import lombok.Getter;
import net.sabafly.mailBox.commands.MailCommands;
import net.sabafly.mailBox.configuration.Config;
import net.sabafly.mailBox.configuration.ConfigLoader;
import net.sabafly.mailBox.database.Database;
import net.sabafly.mailBox.executor.ThreadedQueue;
import net.sabafly.mailBox.listener.PlayerListener;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.menu.MenuManager;
import net.sabafly.mailBox.schedule.ScheduleManager;
import net.sabafly.mailBox.utils.EconomyUtils;
import net.sabafly.mailbox.api.IMailBox;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public final class MailBox extends JavaPlugin implements Listener, IMailBox {

    private final ConfigLoader config;
    private Database database;
    @Getter
    private static ThreadedQueue<Runnable> threadedQueue;
    private static Logger logger;
    private MenuManager menuManager;
    private ScheduleManager scheduleManager;
    private boolean vaultEnabled;

    public MailBox(ConfigLoader config) {
        this.config = config;
    }

    public static Logger logger() {
        return logger;
    }

    @Override
    public void onLoad() {
        logger = getSLF4JLogger();
    }

    @Override
    public void onEnable() {
        threadedQueue = new ThreadedQueue<>("MailBox-Worker-Thread");

        this.database = config.config().database.loadDatabase();
        this.database.setup();

        menuManager = MenuManager.register();

        scheduleManager = new ScheduleManager(this);
        scheduleManager.start();

        new PlayerListener().register(this);

        new MailCommands(this).registerCommands();

        if (!currentVersion().isStable()) {
            logger().warn("===============================");
            logger().warn(" You are running a development version ");
            logger().warn(" May contain bugs and unstable features ");
            logger().warn(" Current version: " + getPluginMeta().getVersion());
            logger().warn(" Please report any issues you find ");
            logger().warn("===============================");
        }

        Bukkit.getScheduler().runTask(this, this::loadVault);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, task -> updateCheck(), 1, 6 * 60 * 60 * 20);
    }

    @Override
    public void onDisable() {
        if (scheduleManager != null) scheduleManager.stop();
        if (database != null) database.close();
        if (threadedQueue != null) threadedQueue.stop();
    }

    public static MailBox getInstance() {
        return getPlugin(MailBox.class);
    }

    @Override
    public @Nullable net.sabafly.mailbox.api.mail.Mail createMail(@NotNull String subject, @NotNull String content, @Nullable User sender, @NotNull User receiver) {
        Mail mail = new Mail(UUID.randomUUID(), sender != null ? sender.id() : null, receiver.id(), subject, content, java.util.Collections.emptyList(), false, java.time.LocalDateTime.now());
        try {
            database().createMail(mail);
            return mail;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public @NotNull User getUser(@NotNull Player player) {
        return database().getUser(player.getUniqueId());
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
                        var versionString = ((java.util.Map<?, ?>) ((java.util.List<?>) raw).getFirst()).get("version_number");
                        final Semver version = new Semver((String) versionString);
                        if (currentVersion().isLowerThan(version)) {
                            if (version.isStable()) {
                                getSLF4JLogger().info("A new version is available");
                                getSLF4JLogger().info("Latest version: {}", versionString);
                            } else {
                                getSLF4JLogger().info("A new development version is available");
                                getSLF4JLogger().info("Development version: {}", versionString);
                                getSLF4JLogger().warn("This version may not be stable and could contain bugs.");
                            }
                            getSLF4JLogger().info("Current version: {}", getPluginMeta().getVersion());
                        } else {
                            getSLF4JLogger().info("No updates available");
                        }
                    }).join();
        } catch (Exception e) {
            getSLF4JLogger().error("Failed to check for updates: {}", e.getLocalizedMessage());
        }
    }

    private @NotNull Semver currentVersion() {
        return new Semver(getPluginMeta().getVersion());
    }

}
