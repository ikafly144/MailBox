package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailUser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

@SuppressWarnings("UnstableApiUsage")
public class MailMenu extends BaseMenu<MailMenu> {

    private int page;

    public MailMenu(Player player) {
        this(player, 1);
    }

    public MailMenu(Player player, int page) {
        super(player, 45, menu -> {
            final MailUser user = database().getUser(player.getUniqueId());
            return miniMessage().deserialize(config().messages.mailMenuTitle + " " + config().messages.page + " " + menu.page + "/" + (database().countMails(user, TriState.NOT_SET) / 27 + 1) + " " + config().messages.mails + " (" + database().countMails(user, TriState.NOT_SET) + "/" + config().mail.maxMailCount + ")");
        });
        this.page = page;
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.previousPage)));
        clickRegistry.setItem(0, arrow, (player, clickType) -> {
            if (clickType.isLeftClick() && page > 1) {
                page--;
                refresh();
            }
        });
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.nextPage)));
        clickRegistry.setItem(8, arrow, (player, clickType) -> {
            if (clickType.isLeftClick() && database().countMails(database().getUser(player.getUniqueId()), TriState.NOT_SET) > page * 27) {
                page++;
                refresh();
            }
        });
        ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < 9; i++) {
            clickRegistry.setItem(9 + i, glassPane);
        }
        int slot = 18;
        for (Mail mail : database().getMails(database().getUser(player.getUniqueId()), TriState.NOT_SET, page).stream().sorted().toList()) {
            clickRegistry.setItem(slot, createMailItem(mail), (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    openMenu(new MailViewerMenu(player, mail, true));
                } else if (clickType.isRightClick() && mail.getAttachments().stream().allMatch(a -> a.received() || a.isExpired())) {
                    database().deleteMail(mail);
                    refresh();
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
            List<Component> lore = config().messages.mailMenuMailLore
                    .replace("{sender}", Optional.ofNullable(mail.getSender()).map(sender -> Bukkit.getOfflinePlayer(sender.uuid()).getName()).orElse(config().messages.systemName))
                    .replace("{time}", mail.getSentTime().toString())
                    .replace("{attachments}", mail.getAttachments().size() + " (" + config().messages.unreceived + " " + mail.getAttachments().stream().filter(a -> !a.received()).count() + ")")
                    .replace("{read}", mail.isRead() ? config().messages.read : config().messages.unread)
                    .transform(s -> Stream.of(s.split("\n")))
                    .filter(s -> !s.isBlank()).map(miniMessage()::deserialize)
                    .collect(Collectors.toCollection(ArrayList::new));
            lore.addFirst(miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionDelete)));
            lore.addFirst(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionOpen)));
            meta.lore(lore);
        });
        return item;
    }

}
