package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.MailBox;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class ItemSetterMenu extends InventoryMenu<ItemSetterMenu> {

    private final InventoryMenu<?> parent;
    private final Consumer<ItemStack> consumer;
    private final ItemStack def;

    public ItemSetterMenu(InventoryMenu<?> parent, Player player, Consumer<ItemStack> consumer, @Nullable ItemStack def) {
        super(player, InventoryType.DROPPER, miniMessage().deserialize(config().messages.attachmentAppendItem), true);
        this.parent = parent;
        this.consumer = consumer;
        this.def = def;
    }

    @Override
    protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
        if (inventory == null) {
            setNextMenu(parent);
            return;
        }
        try {
            Optional.ofNullable(inventory.getTopInventory().getItem(4))
                    .ifPresent(consumer);
        } catch (Exception e) {
            MailBox.logger().error("Error while setting item attachment", e);
        }
        setNextMenu(parent);
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                clickRegistry.setItem(i, glassPane, (player1, clickType) -> {
                });
            }
        }
        if (def != null) {
            clickRegistry.setItem(4, def);
        }
    }
}
