package net.sabafly.mailbox.event;


import net.sabafly.mailbox.inventory.MailBoxInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

public class InventoryEventHandler implements Listener {

    private static InventoryEventHandler instance;

    public static InventoryEventHandler getInstance() {
        if (instance == null) {
            instance = new InventoryEventHandler();
        }
        return instance;
    }

    private InventoryEventHandler() {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        var inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof MailBoxInventory holder))
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        event.setCancelled(true);
        var slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize())
            return;
        if (inventory.getItem(slot) == null)
            return;
        holder.onClick(event.getSlot(), player,event.getClick());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!(event.getDestination().getHolder() instanceof MailBoxInventory))
            return;
        event.setItem(ItemStack.empty());
        event.setCancelled(true);
    }

}
