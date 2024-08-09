package net.sabafly.mailbox.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class AbstractInventory implements InventoryHolder {

    private final Inventory inventory;

    protected AbstractInventory(int size, Component name) {
        this.inventory = Bukkit.createInventory(this, size, name);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }
}
