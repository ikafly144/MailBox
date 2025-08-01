package net.sabafly.mailBox.listener;

import net.sabafly.mailBox.mail.MailUser;
import net.sabafly.mailBox.schedule.ScheduleManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import static net.sabafly.mailBox.MailBox.database;

public class PlayerListener implements Listener {

    private Plugin plugin;

    public PlayerListener() {
    }

    public void register(@NotNull Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent event) {
        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            MailUser user = database().getUser(event.getPlayer().getUniqueId());
            ScheduleManager.checkNotify(event.getPlayer(), user, true);
        });
    }

}
