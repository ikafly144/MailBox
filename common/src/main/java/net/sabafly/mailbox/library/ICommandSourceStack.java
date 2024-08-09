package net.sabafly.mailbox.library;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public interface ICommandSourceStack extends CommandSourceStack {

    void sendSuccess(Supplier<Component> message, boolean broadcastToOps);

    void sendFailure(Component message);

    void sendSystemMessage(Component message);

}
