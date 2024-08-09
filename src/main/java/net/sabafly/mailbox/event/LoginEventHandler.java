package net.sabafly.mailbox.event;

import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.sabafly.mailbox.MailBox;
import net.sabafly.mailbox.configurations.PluginConfiguration;
import net.sabafly.mailbox.type.MailBoxEntry;
import net.sabafly.mailbox.utils.ItemUtil;
import net.sabafly.mailbox.utils.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class LoginEventHandler implements Listener {

    private static final Logger LOGGER = LogUtil.getLogger();

    private static LoginEventHandler instance;

    public static LoginEventHandler getInstance() {
        if (instance == null) {
            instance = new LoginEventHandler();
        }
        return instance;
    }

    private LoginEventHandler() {
    }

    private static boolean isFirstLogin = true;

    @EventHandler(ignoreCancelled = true)
    public void onLogin(PlayerJoinEvent event) {
        if (event.isAsynchronous())
            return;
        var player = event.getPlayer();
        final var config = PluginConfiguration.get();
        // last login LocalDateTime
        checkNewMail(player, config);
        if (isFirstLogin) {
            isFirstLogin = false;
            Bukkit.getServer().getScheduler().runTaskTimer(MailBox.getPlugin(MailBox.class), bukkitTask -> {
                final var c = PluginConfiguration.get();
                Bukkit.getServer().getOnlinePlayers().forEach(p -> LoginEventHandler.checkNewMail(p, c));
            }, 20L * 60, 20L * 60);
        }
    }

    private static void checkNewMail(@NotNull Player player, @NotNull PluginConfiguration config) {
        var container = player.getPersistentDataContainer();
        var lastCheckedMilli = container.getOrDefault(LAST_CHECKED, PersistentDataType.LONG, 0L);
        var lastChecked = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastCheckedMilli), ZoneId.systemDefault());
        container.remove(LAST_CHECKED);
        container.set(LAST_CHECKED, PersistentDataType.LONG, System.currentTimeMillis());
        LOGGER.info("Checking new mail for %s (last check: %s)".formatted(player.getName(), lastChecked.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        config.mailSchedules.stream()
                .filter(schedule -> schedule.enabled && (schedule.end == null || schedule.end.isAfter(LocalDateTime.now())))
                .filter(schedule -> schedule.start != null && schedule.start.isAfter(lastChecked) && schedule.start.isBefore(LocalDateTime.now()))
                .map(schedule -> MailBoxEntry.create(ItemUtil.parseItem(schedule.item), JSONComponentSerializer.json().deserialize(schedule.name), JSONComponentSerializer.json().deserialize(schedule.description), schedule.action))
                .forEach(mail -> Bukkit.getScheduler().runTaskLater(MailBox.getPlugin(MailBox.class), () -> {
                    LOGGER.info("Sending mail [%s] to %s".formatted(PlainTextComponentSerializer.plainText().serialize(mail.getName()), player.getName()));
                    mail.send(player);
                }, 20L));
    }

    private static final NamespacedKey LAST_CHECKED = new NamespacedKey(MailBox.getPlugin(MailBox.class), "last_checked");

}
