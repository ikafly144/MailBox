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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class BaseMenu<T extends BaseMenu<T>> {

    protected final Player player;
    private final Function<T, Inventory> inventorySupplier;
    private @NotNull Inventory inventory;
    private final ClickRegistry clickRegistry = new ClickRegistry(this);
    @Getter
    private @Nullable BaseMenu<?> nextMenu;
    @Getter
    private final boolean moveable;
    private final @NotNull Function<@NotNull T, @NotNull Component> title;

    public BaseMenu(Player player, int size, Component title) {
        this(player, size, title, false);
    }

    public BaseMenu(Player player, int size, Component title, boolean moveable) {
        this(player, size, menu -> title, moveable);
    }

    public BaseMenu(Player player, int i, @NotNull Function<@NotNull T, @NotNull Component> title) {
        this(player, i, title, false);
    }

    public BaseMenu(Player player, int size, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable) {
        this(player, menu -> Bukkit.createInventory(new MenuHolder(menu), size, title.apply(menu)), title, moveable);
    }

    public BaseMenu(Player player, InventoryType type, Component title) {
        this(player, type, title, false);
    }

    public BaseMenu(Player player, InventoryType type, Component title, boolean moveable) {
        this(player, type, menu -> title, moveable);
    }

    public BaseMenu(Player player, InventoryType type, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable) {
        this(player, menu -> Bukkit.createInventory(new MenuHolder(menu), type, title.apply(menu)), title, moveable);
    }

    private BaseMenu(Player player, Function<T, Inventory> inventory, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable) {
        this(player, inventory, title, moveable, null);
    }

    @SuppressWarnings("unchecked")
    private BaseMenu(Player player, Function<T, Inventory> inventory, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable, @Nullable BaseMenu<?> nextMenu) {
        this.player = player;
        this.inventorySupplier = inventory;
        this.moveable = moveable;
        this.title = title;
        this.inventory = inventory.apply((T) this);
        this.nextMenu = nextMenu;
    }

    public void open() {
        ThreadUtils.runSync(() -> {
            refresh();
            player.openInventory(inventory);
        });
    }

    private boolean refreshing = false;

    @SuppressWarnings("unchecked")
    protected void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        clickRegistry.clickMap.clear();
        inventory.clear();
        inventory = inventorySupplier.apply((T) this);
        setItems(clickRegistry);
        player.openInventory(inventory);
    }

    public int size() {
        return inventory.getSize();
    }

    abstract void setItems(@NotNull ClickRegistry clickRegistry);

    protected final void openMenu(@NotNull BaseMenu<?> menu) {
        setNextMenu(menu);
        player.closeInventory();
    }

    protected final void setNextMenu(@NotNull BaseMenu<?> menu) {
        if (nextMenu != null) {
            return;
        }
        nextMenu = menu;
    }

    public final void onCloseComplete() {
        if (refreshing) {
            refreshing = false;
            return;
        }
        if (nextMenu != null) {
            nextMenu.open();
            nextMenu = null;
        }
    }

    @ApiStatus.Internal
    public void callClose(Player player, InventoryView inventory) {
        if (refreshing) {
            return;
        }
        onClose(player, inventory);
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


    @SuppressWarnings("unchecked")
    public Component getTitle() {
        return title.apply((T) this);
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
        private final BaseMenu<?> menu;
        private final Map<Integer, BiConsumer<@NotNull Player, @NotNull ClickType>> clickMap = new HashMap<>();

        public ClickRegistry(BaseMenu<?> menu) {
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

        private final BaseMenu<?> menu;

        private MenuHolder(BaseMenu<?> menu) {
            this.menu = menu;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return menu.inventory;
        }

        public BaseMenu<?> menu() {
            return menu;
        }

    }

}
