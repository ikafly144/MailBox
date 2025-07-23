package net.sabafly.mailBox.menu;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface Menu {

    void open();

    void callClose(@NotNull Player player);

}
