package net.sabafly.mailbox.library;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.inventory.ItemStack;

public interface LibNMS {

    ItemStack parseItemStack(String itemStack) throws CommandSyntaxException;

    String formatItemStack(ItemStack itemStack);

    ICommandSourceStack unwrapCommandSourceStack(CommandSourceStack commandSourceStack);

    CommandSyntaxException notPlayerException();

}
