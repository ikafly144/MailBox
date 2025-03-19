package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.attachments.CommandAttachment;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;

@SuppressWarnings("UnstableApiUsage")
public class AttachmentCommandMenu extends BaseMenu<AttachmentCommandMenu> {

    private final CreateMailMenu.AttachmentMenu parent;
    private final Consumer<CommandAttachment> consumer;

    private @Nullable String name = null;
    private @Nullable String command = null;
    private @NotNull ItemStack displayItem = new ItemStack(Material.COMMAND_BLOCK);

    public AttachmentCommandMenu(CreateMailMenu.AttachmentMenu parent, Player player, Consumer<CommandAttachment> consumer) {
        super(player, 9, miniMessage().deserialize(config().messages.attachmentAppendCommand));
        this.parent = parent;
        this.consumer = consumer;
    }

    @Override
    protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
        setNextMenu(parent);
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        final ItemStack nameTag = getNameTag();
        clickRegistry.setItem(4, nameTag, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new AnvilSetterMenu(this, player, miniMessage().deserialize(config().messages.setName), (s) -> name = s, name));
            }
        });
        final ItemStack commandBlock = getCommandBlock();
        clickRegistry.setItem(3, commandBlock, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new AnvilSetterMenu(this, player, miniMessage().deserialize(config().messages.setCommand), (s) -> command = s, command));
            }
        });
        final ItemStack display = new ItemStack(displayItem.getType());
        display.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.displayItem));
            meta.lore(List.of(miniMessage().deserialize(config().messages.leftClickTo
                    .replace("{action}", config().messages.clickActionSet))));
        });
        clickRegistry.setItem(5, display, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new ItemSetterMenu(this, player, item -> displayItem = item, displayItem));
            }
        });
        ItemStack limeWool = new ItemStack(Material.LIME_WOOL);
        limeWool.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.append));
            meta.lore(List.of(miniMessage().deserialize(config().messages.leftClickTo
                    .replace("{action}", config().messages.clickActionCreate))));
        });
        clickRegistry.setItem(8, limeWool, (player, clickType) -> {
            if (clickType.isLeftClick() && name != null && command != null) {
                try {
                    consumer.accept(new CommandAttachment(
                            name,
                            false,
                            RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(displayItem.getType().key()),
                            null,
                            config().mail.getExpirationTime(),
                            command
                    ));
                } catch (Exception e) {
                    MailBox.logger().error("Error while setting command attachment", e);
                }
                openMenu(parent);
            }
        });
    }

    private @NotNull ItemStack getCommandBlock() {
        ItemStack commandBlock = new ItemStack(Material.COMMAND_BLOCK);
        commandBlock.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.setCommand));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo
                            .replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.commandValue,
                            TagResolver.builder()
                                    .tag("command", Tag.inserting(Optional.ofNullable(command)
                                            .map(s -> (Component) plainText().deserialize(s))
                                            .orElse(miniMessage().deserialize(config().messages.noValue))))
                                    .build())
            ));
        });
        return commandBlock;
    }

    private @NotNull ItemStack getNameTag() {
        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        nameTag.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.setName));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo
                            .replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.nameValue,
                            TagResolver.builder()
                                    .tag("name", Tag.inserting(Optional.ofNullable(name)
                                            .map(s -> (Component) plainText().deserialize(s))
                                            .orElse(miniMessage().deserialize(config().messages.noValue))))
                                    .build())
            ));
        });
        return nameTag;
    }

}