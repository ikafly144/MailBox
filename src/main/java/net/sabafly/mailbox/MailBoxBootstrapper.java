package net.sabafly.mailbox;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.sabafly.mailbox.commands.MailBoxCommands;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class MailBoxBootstrapper implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        var lifecycle = context.getLifecycleManager();
        lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, new MailBoxCommands());
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new MailBox();
    }
}
