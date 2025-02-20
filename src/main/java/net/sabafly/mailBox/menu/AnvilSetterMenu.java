package net.sabafly.mailBox.menu;

import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class AnvilSetterMenu extends BaseMenu<AnvilSetterMenu> implements SetResult<String> {

    private final Consumer<String> consumer;
    private final BaseMenu<?> parent;
    @Nullable
    private String result = null;
    private final String def;

    public AnvilSetterMenu(BaseMenu<?> parent, Player player, Component title, Consumer<String> consumer) {
        this(parent, player, title, consumer, null);
    }

    public AnvilSetterMenu(BaseMenu<?> parent, Player player, Component title, Consumer<String> consumer, @Nullable String def) {
        super(player, InventoryType.ANVIL, title);
        this.consumer = consumer;
        this.parent = parent;
        this.def = def;
    }

    @Override
    protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
        if (result != null && !result.isBlank()) {
            try {
                consumer.accept(result);
            } catch (Exception e) {
                MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> player.sendMessage(Component.text("Error: " + e.getMessage()))));
                MailBox.logger().error("Error while setting anvil result", e);
            }
        }
        setNextMenu(parent);
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack paper = new ItemStack(Material.PAPER);
        paper.editMeta(meta -> meta.itemName(def == null || def.isBlank() ? Component.empty() : Component.text(def)));
        clickRegistry.setItem(0, paper);
        clickRegistry.register(2, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(p::closeInventory));
            }
        });
    }

    @Override
    public void setResult(String result) {
        if (result == null || result.isBlank()) {
            return;
        }
        this.result = result;
    }
}
