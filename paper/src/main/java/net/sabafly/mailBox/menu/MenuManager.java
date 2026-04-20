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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MenuManager implements Listener {

    private static MenuManager INSTANCE;

    private final Plugin plugin;

    public MenuManager() {
        this.plugin = MailBox.getInstance();
    }

    public static MenuManager register() {
        if (INSTANCE == null) {
            INSTANCE = new MenuManager();
            INSTANCE.plugin.getServer().getPluginManager().registerEvents(INSTANCE, INSTANCE.plugin);
        }
        return INSTANCE;
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
        if (event.getInventory().getHolder() instanceof InventoryMenu.MenuHolder holder) {
            if (event.getReason() == InventoryCloseEvent.Reason.DISCONNECT) {
                Menu m = holder.menu();
                while ((m instanceof InventoryMenu<?> inv) && inv.getNextMenu() != null) {
                    inv.callClose(player, event.getView());
                    m = inv.getNextMenu();
                }
                m.callClose(player);
                return;
            }
            CompletableFuture<Void> future = new CompletableFuture<>();
            MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(player, () -> {
                try {
                    holder.menu().callClose(player, event.getView());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    MailBox.logger().error("Error while closing menu", e);
                    return;
                }
                future.complete(null);
            }));
            future.thenRun(holder.menu()::onCloseComplete);
            Bukkit.getAsyncScheduler().runNow(plugin, _ -> future.join());
        }
    }

}
