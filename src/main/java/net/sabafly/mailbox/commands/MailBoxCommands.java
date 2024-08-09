package net.sabafly.mailbox.commands;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.jetbrains.annotations.NotNull;

public class MailBoxCommands implements LifecycleEventHandler<ReloadableRegistrarEvent<Commands>> {

    @Override
    public void run(@NotNull ReloadableRegistrarEvent<Commands> event) {
        Commands commands = event.registrar();
        commands.register(new MailBoxCommand().command());
        commands.register(new SendMailCommand().command());
    }

}
