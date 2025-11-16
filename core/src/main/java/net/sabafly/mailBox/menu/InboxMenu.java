package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailUser;
import net.sabafly.mailBox.utils.DateUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

@SuppressWarnings("UnstableApiUsage")
public class InboxMenu extends InventoryMenu<InboxMenu> {

    private int page;
    private long cooldown = 0;

    private static final NamespacedKey MAIL_INBOX_KEY = new NamespacedKey("mailbox", "inbox_menu");

    public InboxMenu(Player player) {
        this(player, 1);
    }

    public InboxMenu(Player player, int page) {
        super(player, 45, menu -> {
            final MailUser user = database().getUser(player.getUniqueId());
            return miniMessage().deserialize(
                    config().messages.inboxMenuTitle
                            .replaceAll("\\{unread_count}", Matcher.quoteReplacement(String.valueOf(database().countMails(user, TriState.FALSE))))
                            .replaceAll("\\{total_count}", Matcher.quoteReplacement(String.valueOf(database().countMails(user, TriState.NOT_SET))))
                            .replaceAll("\\{page}", Matcher.quoteReplacement(String.valueOf(menu.page)))
                            .replaceAll("\\{total_pages}", Matcher.quoteReplacement(String.valueOf((int) Math.ceil(database().countMails(user, TriState.NOT_SET) / 27.0))))
                            .replaceAll("\\{max_mail_count}", Matcher.quoteReplacement(String.valueOf(config().mail.maxMailCount)))
                            .replaceAll("\\{player_name}", Matcher.quoteReplacement(player.getName()))
            );
        });
        this.page = page;
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack leftArrow = Bukkit.getItemFactory().createItemStack(config().leftArrowItem);
        leftArrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.previousPage)));
        if (page > 1) clickRegistry.setItem(0, leftArrow, (player, clickType) -> {
            if (clickType.isLeftClick() && page > 1) {
                page--;
                refresh();
            }
        });
        ItemStack rightArrow = Bukkit.getItemFactory().createItemStack(config().rightArrowItem);
        rightArrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.nextPage)));
        if (database().countMails(database().getUser(player.getUniqueId()), TriState.NOT_SET) > page * 27) {
            clickRegistry.setItem(8, rightArrow, (player, clickType) -> {
                if (clickType.isLeftClick() && database().countMails(database().getUser(player.getUniqueId()), TriState.NOT_SET) > page * 27) {
                    page++;
                    refresh();
                }
            });
        }
        ItemStack refreshItem = ItemStack.of(Material.WIND_CHARGE);
        refreshItem.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.refreshButton)));
        refreshItem.editMeta(meta -> {
            var cooldown = meta.getUseCooldown();
            cooldown.setCooldownGroup(MAIL_INBOX_KEY);
            meta.setUseCooldown(cooldown);
        });
        if (System.currentTimeMillis() <= this.cooldown)
            player.setCooldown(MAIL_INBOX_KEY, (int) ((this.cooldown - System.currentTimeMillis()) / 50));
        clickRegistry.setItem(4, refreshItem, (player, clickType) -> {
            if (clickType.isLeftClick() && System.currentTimeMillis() > this.cooldown) {
                this.cooldown = System.currentTimeMillis() + 5000;
                refresh();
            }
        });
        ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < 9; i++) {
            clickRegistry.setItem(9 + i, glassPane);
        }
        int slot = 18;
        database().deleteAllUserNotification(database().getUser(player.getUniqueId()));
        for (Mail mail : database().getMails(database().getUser(player.getUniqueId()), TriState.NOT_SET, page).stream().sorted().toList().reversed()) {
            clickRegistry.setItem(slot, createMailItem(mail), (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    openMenu(new MailViewerMenu(player, mail, true));
                } else if (clickType.isRightClick() && mail.getAttachmentsInternal().stream().allMatch(a -> a.opened() || a.isExpired())) {
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
                   .replaceAll("\\{sender}", Matcher.quoteReplacement(Optional.ofNullable(mail.getSender()).map(sender -> Bukkit.getOfflinePlayer(sender.uuid()).getName()).orElse(config().messages.systemName)))
                   .replaceAll("\\{time}", Matcher.quoteReplacement(DateUtils.format(mail.getSentTime())))
                    .replaceAll("\\{attachments}", Matcher.quoteReplacement(mail.attachments().size() + " (" + config().messages.unreceived + " " + mail.getAttachmentsInternal().stream().filter(a -> !a.opened() && !a.isExpired()).count() + ")"))
                   .replaceAll("\\{read}", Matcher.quoteReplacement(mail.isRead() ? config().messages.read : config().messages.unread))
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
