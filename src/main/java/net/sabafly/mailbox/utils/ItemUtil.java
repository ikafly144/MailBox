package net.sabafly.mailbox.utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.sabafly.mailbox.library.LibNMSAccessor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class ItemUtil {

    private ItemUtil() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static String formatItem(ItemStack item) {
        return LibNMSAccessor.get().formatItemStack(item);
    }

    public static ItemStack parseItem(String item) {
        try {
            return LibNMSAccessor.get().parseItemStack(item);
        } catch (CommandSyntaxException e) {
            return ItemStack.of(Material.STONE);
        }
    }

}
