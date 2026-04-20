package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.MailBox;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class SimpleMenu implements Menu {

    @NotNull
    protected Player viewer;

    protected SimpleMenu(@NotNull Player viewer) {
        this.viewer = viewer;
    }

    protected abstract void openMenu(@NotNull Menu menu);

}
