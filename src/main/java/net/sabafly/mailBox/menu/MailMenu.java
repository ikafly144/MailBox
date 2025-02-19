package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.mail.Mail;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

@SuppressWarnings("UnstableApiUsage")
public class MailMenu extends BaseMenu {

    private final int page;

    public MailMenu(Player player) {
        this(player, 1);
    }

    public MailMenu(Player player, int page) {
        super(player, 45, miniMessage().deserialize(config().messages.mailMenuTitle + " " + page));
        this.page = page;
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack arrow = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.ARROW).createItemStack();
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.previousPage)));
        clickRegistry.setItem(0, arrow, (player, clickType) -> {
            if (clickType.isLeftClick() && page > 1) {
                openMenu(new MailMenu(player, page - 1));
            }
        });
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.nextPage)));
        clickRegistry.setItem(8, arrow, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new MailMenu(player, page + 1));
            }
        });
        ItemStack glassPane = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.BLACK_STAINED_GLASS_PANE).createItemStack();
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < 9; i++) {
            clickRegistry.setItem(9 + i, glassPane);
        }
        int slot = 18;
        for (Mail mail : database().getMails(database().getUser(player.getUniqueId()), TriState.NOT_SET, page).stream().sorted().toList()) {
            clickRegistry.setItem(slot, createMailItem(mail), (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    openMenu(new MailViewerMenu(player, mail, true));
                }
            });
            slot++;
        }
    }

    private ItemStack createMailItem(Mail mail) {
        ItemStack item = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.BOOK).createItemStack();
        item.editMeta(meta -> {
            meta.itemName(plainText().deserialize(mail.getTitle()));
            if (!mail.isRead()) {
                meta.addEnchant(Enchantment.INFINITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(config().messages.mailMenuMailLore
                    .replace("{sender}", Optional.ofNullable(mail.getSender()).map(sender -> Bukkit.getOfflinePlayer(sender.uuid()).getName()).orElse(config().messages.systemName))
                    .replace("{time}", mail.getSentTime().toString())
                    .replace("{attachments}", mail.getAttachments().size() + "")
                    .replace("{read}", mail.isRead() ? config().messages.read : config().messages.unread)
                    .transform(s -> List.of(s.split("\n")))
                    .stream().filter(s -> !s.isBlank()).map(miniMessage()::deserialize).toList());
        });
        return item;
    }

}
