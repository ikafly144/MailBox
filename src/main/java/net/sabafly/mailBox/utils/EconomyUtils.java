package net.sabafly.mailBox.utils;

import net.sabafly.mailBox.MailBox;

import java.util.Objects;

public class EconomyUtils {

    public static net.milkbowl.vault.economy.Economy getEconomy() {
        return Objects.requireNonNull(MailBox.getInstance().getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class)).getProvider();
    }

}
