package net.sabafly.mailBox.menu;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public abstract class BaseMenu {

    protected final Player player;
    private final Inventory inventory;
    private final ClickRegistry clickRegistry = new ClickRegistry(this);
    @Getter
    private BaseMenu nextMenu = null;
    @Getter
    private final boolean moveable;

    public BaseMenu(Player player, int size, Component title) {
        this(player, size, title, false);
    }

    public BaseMenu(Player player, int size, Component title, boolean moveable) {
        this.player = player;
        this.inventory = Bukkit.createInventory(new MenuHolder(this), size, title);
        this.moveable = moveable;
    }

    public BaseMenu(Player player, InventoryType type, Component title) {
        this(player, type, title, false);
    }

    public BaseMenu(Player player, InventoryType type, Component title, boolean moveable) {
        this.player = player;
        this.inventory = Bukkit.createInventory(new MenuHolder(this), type, title);
        this.moveable = moveable;
    }

    public void open() {
        ThreadUtils.runSync(() -> {
            refresh();
            player.openInventory(inventory);
        });
    }

    protected void refresh() {
        clickRegistry.clickMap.clear();
        inventory.clear();
        setItems(clickRegistry);
    }

    public int size() {
        return inventory.getSize();
    }

    abstract void setItems(@NotNull ClickRegistry clickRegistry);

    protected final void openMenu(@NotNull BaseMenu menu) {
        setNextMenu(menu);
        player.closeInventory();
    }

    protected final void setNextMenu(@NotNull BaseMenu menu) {
        if (nextMenu != null) {
            return;
        }
        nextMenu = menu;
    }

    public final void onCloseComplete() {
        if (nextMenu != null) {
            nextMenu.open();
            nextMenu = null;
        }
    }

    protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
    }

    public final void onClick(@NotNull InventoryClickEvent event) {
        event.setCancelled(!moveable || clickRegistry.exist(event.getSlot()));
        Bukkit.getScheduler().runTask(MailBox.getInstance(), () -> clickRegistry.call((Player) event.getWhoClicked(), switch (event.getClick()) {
            case LEFT -> ClickType.LEFT;
            case RIGHT -> ClickType.RIGHT;
            case SHIFT_LEFT -> ClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> ClickType.SHIFT_RIGHT;
            default -> ClickType.UNKNOWN;
        }, event.getSlot()));
    }

    public enum ClickType {
        LEFT,
        RIGHT,
        SHIFT_LEFT,
        SHIFT_RIGHT,
        UNKNOWN;

        public boolean isLeftClick() {
            return this == LEFT || this == SHIFT_LEFT;
        }

        public boolean isRightClick() {
            return this == RIGHT || this == SHIFT_RIGHT;
        }

        public boolean isShiftClick() {
            return this == SHIFT_LEFT || this == SHIFT_RIGHT;
        }

    }

    protected static class ClickRegistry {
        private final BaseMenu menu;
        private final Map<Integer, BiConsumer<@NotNull Player, @NotNull ClickType>> clickMap = new HashMap<>();

        public ClickRegistry(BaseMenu menu) {
            this.menu = menu;
        }

        public void register(int slot, BiConsumer<@NotNull Player, @NotNull ClickType> onClick) {
            clickMap.put(slot, onClick);
        }

        public void setItem(int slot, ItemStack item, BiConsumer<@NotNull Player, @NotNull ClickType> onClick) {
            menu.inventory.setItem(slot, item);
            clickMap.put(slot, onClick);
        }

        public void setItem(int slot, @NotNull ItemStack item) {
            menu.inventory.setItem(slot, item);
        }

        private void call(@NotNull Player player, @NotNull ClickType clickType, int slot) {
            Optional.ofNullable(clickMap.get(slot)).ifPresent(consumer -> consumer.accept(player, clickType));
        }

        public boolean exist(int slot) {
            return clickMap.containsKey(slot);
        }
    }

    public static class MenuHolder implements InventoryHolder {

        private final BaseMenu menu;

        private MenuHolder(BaseMenu menu) {
            this.menu = menu;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return menu.inventory;
        }

        public BaseMenu menu() {
            return menu;
        }

    }

}
