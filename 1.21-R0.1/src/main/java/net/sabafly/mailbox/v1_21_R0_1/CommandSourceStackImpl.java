package net.sabafly.mailbox.v1_21_R0_1;

import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.sabafly.mailbox.library.ICommandSourceStack;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CommandSourceStackImpl implements ICommandSourceStack {

    private final net.minecraft.commands.CommandSourceStack nmsInstance;

    public static CommandSourceStackImpl of(CommandSourceStack nmsInstance) {
        return new CommandSourceStackImpl(nmsInstance);
    }

    private CommandSourceStackImpl(CommandSourceStack nmsInstance) {
        this.nmsInstance = nmsInstance;
    }

    @Override
    public void sendSuccess(Supplier<Component> message, boolean broadcastToOps) {
        this.nmsInstance.sendSuccess(() -> io.papermc.paper.adventure.PaperAdventure.asVanilla(message.get()), broadcastToOps);
    }

    @Override
    public void sendFailure(Component message) {
        this.nmsInstance.sendFailure(io.papermc.paper.adventure.PaperAdventure.asVanilla(message));
    }

    @Override
    public void sendSystemMessage(Component message) {
        this.nmsInstance.sendSystemMessage(io.papermc.paper.adventure.PaperAdventure.asVanilla(message));
    }

    @Override
    public @NotNull Location getLocation() {
        return CraftLocation.toBukkit(this.nmsInstance.getPosition());
    }

    @Override
    public @NotNull CommandSender getSender() {
        return this.nmsInstance.getBukkitSender();
    }

    @Override
    public @Nullable Entity getExecutor() {
        return this.nmsInstance.getBukkitEntity();
    }
}
