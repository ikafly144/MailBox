package net.sabafly.mailbox;

import net.sabafly.mailbox.commands.AbstractCommand;
import net.sabafly.mailbox.configurations.Configurations;
import net.sabafly.mailbox.event.InventoryEventHandler;
import net.sabafly.mailbox.event.LoginEventHandler;
import net.sabafly.mailbox.utils.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.configurate.ConfigurateException;

import java.util.logging.Logger;

public final class MailBox extends JavaPlugin {

    private final Logger LOGGER = LogUtil.getLogger();

    @Override
    public void onEnable() {
        try {
            Configurations.loadPluginConfiguration(getDataPath().resolve("config.yml"));
        } catch (ConfigurateException e) {
            LOGGER.severe("Failed to load configuration file!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AbstractCommand.enable();
        getServer().getPluginManager().registerEvents(InventoryEventHandler.getInstance(), this);
        getServer().getPluginManager().registerEvents(LoginEventHandler.getInstance(), this);
        getLogger().info("MailBox has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MailBox has been disabled!");
    }
}
