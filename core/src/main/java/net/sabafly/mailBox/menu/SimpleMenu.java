package net.sabafly.mailBox.menu;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class SimpleMenu implements Menu {

    @NotNull
    protected Player player;

    protected SimpleMenu(@NotNull Player player) {
        this.player = player;
    }

    protected abstract void openMenu(@NotNull Menu menu);

}
