package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.attachments.ItemAttachment;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class AttachmentItemMenu extends InventoryMenu<AttachmentItemMenu> {

    private final CreateMailMenu.AttachmentMenu parent;
    private final Consumer<ItemAttachment> consumer;

    public AttachmentItemMenu(CreateMailMenu.AttachmentMenu parent, Player player, Consumer<ItemAttachment> consumer) {
        super(player, InventoryType.DROPPER, miniMessage().deserialize(config().messages.attachmentAppendItem), true);
        this.parent = parent;
        this.consumer = consumer;
    }

    @Override
    protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
        if (inventory == null) {
            setNextMenu(parent);
            return;
        }
        try {
            Optional.ofNullable(inventory.getTopInventory().getItem(4))
                    .map(item -> new ItemAttachment(item, false, null, Duration.ofSeconds(0)))
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
    }
}
