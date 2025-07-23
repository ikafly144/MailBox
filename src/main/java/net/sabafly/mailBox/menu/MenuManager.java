package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;

public class MenuManager implements Listener {

    private final Plugin plugin;

    public MenuManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        boolean startPlayer = event.getView().getBottomInventory() == event.getView().getInventory(event.getRawSlot());
        if (event.getClickedInventory() == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getClickedInventory().getHolder() instanceof InventoryMenu.MenuHolder menu) {
            menu.menu().onClick(event);
        } else if (startPlayer && event.getInventory().getHolder() instanceof InventoryMenu.MenuHolder menu && !menu.menu().isMoveable()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof InventoryMenu.MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof InventoryMenu.MenuHolder menu) {
            if (event.getReason() == InventoryCloseEvent.Reason.DISCONNECT) {
                InventoryMenu<?> m = menu.menu();
                while (m.getNextMenu() != null) {
                    m = m.getNextMenu();
                    m.callClose(player);
                }
                return;
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> {
                try {
                    menu.menu().callClose(player);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }
                future.complete(null);
            }));
            future.thenRun(menu.menu()::onCloseComplete);
            Bukkit.getAsyncScheduler().runNow(plugin, t -> future.join());
        }
    }

}
