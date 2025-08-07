package net.sabafly.mailBox.menu;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class DialogMenu extends SimpleMenu {

    protected DialogMenu(@NotNull Player player) {
        super(player);
    }

    @Override
    protected void openMenu(@NotNull Menu menu) {
        menu.open();
    }

    @Override
    public void callClose(@NotNull Player player) {
    }
}
