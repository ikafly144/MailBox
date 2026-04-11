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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class InventoryMenu<T extends InventoryMenu<T>> extends SimpleMenu implements Menu {

    private final Function<T, Inventory> inventorySupplier;
    private @NotNull Inventory inventory;
    private final ClickRegistry clickRegistry = new ClickRegistry(this);
    @Getter
    private @Nullable Menu nextMenu;
    @Getter
    private final boolean moveable;
    private final @NotNull Function<@NotNull T, @NotNull Component> title;

    public InventoryMenu(Player viewer, int size, Component title) {
        this(viewer, size, title, false);
    }

    public InventoryMenu(Player viewer, int size, Component title, boolean moveable) {
        this(viewer, size, _ -> title, moveable);
    }

    public InventoryMenu(Player viewer, int i, @NotNull Function<@NotNull T, @NotNull Component> title) {
        this(viewer, i, title, false);
    }

    public InventoryMenu(Player viewer, int size, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable) {
        this(viewer, menu -> Bukkit.createInventory(new MenuHolder(menu), size, title.apply(menu)), title, moveable);
    }

    public InventoryMenu(Player viewer, InventoryType type, Component title) {
        this(viewer, type, title, false);
    }

    public InventoryMenu(Player viewer, InventoryType type, Component title, boolean moveable) {
        this(viewer, type, _ -> title, moveable);
    }

    public InventoryMenu(Player viewer, InventoryType type, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable) {
        this(viewer, menu -> Bukkit.createInventory(new MenuHolder(menu), type, title.apply(menu)), title, moveable);
    }

    private InventoryMenu(Player viewer, Function<T, Inventory> inventory, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable) {
        this(viewer, inventory, title, moveable, null);
    }

    @SuppressWarnings("unchecked")
    private InventoryMenu(Player viewer, Function<T, Inventory> inventory, @NotNull Function<@NotNull T, @NotNull Component> title, boolean moveable, @Nullable InventoryMenu<?> nextMenu) {
        super(viewer);
        this.inventorySupplier = inventory;
        this.moveable = moveable;
        this.title = title;
        this.inventory = inventory.apply((T) this);
        this.nextMenu = nextMenu;
    }

    public void open() {
        ThreadUtils.runSync(() -> {
            refresh();
            viewer.openInventory(inventory);
        });
    }

    private boolean refreshing = false;

    @SuppressWarnings("unchecked")
    protected void refresh() {
        try {
            if (refreshing) {
                return;
            }
            refreshing = true;
            clickRegistry.clickMap.clear();
            inventory.clear();
            inventory = inventorySupplier.apply((T) this);
            setItems(clickRegistry);
            viewer.openInventory(inventory);
        } catch (Exception e) {
            MailBox.logger().error("Error while refreshing menu", e);
            throw new RuntimeException(e);
        }
    }

    public int size() {
        return inventory.getSize();
    }

    abstract void setItems(@NotNull ClickRegistry clickRegistry);

    @Override
    protected final void openMenu(@NotNull Menu menu) {
        setNextMenu(menu);
        viewer.closeInventory();
    }

    protected final void setNextMenu(@NotNull Menu menu) {
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

    @Override
    public void callClose(@NotNull Player player) {
        if (refreshing) {
            return;
        }
        onClose(player, player.getOpenInventory());
    }

    public void callClose(@NotNull Player player, @Nullable InventoryView inventory) {
        if (refreshing) {
            return;
        }
        onClose(player, inventory);
    }

    protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
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
        private final InventoryMenu<?> menu;
        private final Map<Integer, BiConsumer<@NotNull Player, @NotNull ClickType>> clickMap = new HashMap<>();

        public ClickRegistry(InventoryMenu<?> menu) {
            this.menu = menu;
        }

        public void register(int slot, BiConsumer<@NotNull Player, @NotNull ClickType> onClick) {
            clickMap.put(slot, onClick);
        }

        public void setItem(int slot, ItemStack item, BiConsumer<@NotNull Player, @NotNull ClickType> onClick) {
            menu.inventory.setItem(slot, item);
            register(slot, onClick);
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

        private final InventoryMenu<?> menu;

        private MenuHolder(InventoryMenu<?> menu) {
            this.menu = menu;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return menu.inventory;
        }

        public InventoryMenu<?> menu() {
            return menu;
        }

    }

}
