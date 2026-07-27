package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.utils.DateUtils;
import net.sabafly.mailBox.utils.ThreadUtils;
import net.sabafly.mailbox.api.mail.User;
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
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.messages;
import static net.sabafly.mailBox.MailBox.database;

@SuppressWarnings("UnstableApiUsage")
public class InboxMenu extends InventoryMenu<InboxMenu> {

    private final User owner;
    private int page;
    private long cooldown = 0;

    private static final NamespacedKey MAIL_INBOX_KEY = new NamespacedKey("mailbox", "inbox_menu");

    public InboxMenu(Player viewer, @NotNull User owner) {
        this(viewer, owner, 1);
    }

    public InboxMenu(Player viewer, @NotNull User owner, int page) {
        this.page = page;
        this.owner = owner;
        super(viewer, 45, menu -> {
            var user = database().getUser(viewer.getUniqueId());
            if (user == null)
                throw new IllegalStateException("User not found");
            var footer = new StringBuilder();
            if (!owner.id().equals(viewer.identity().uuid()))
                footer.append(" ").append(messages().inboxOwner.replace("{owner}", owner.name()));
            return miniMessage().deserialize(
                    (messages().inboxMenuTitle + footer)
                            .replaceAll("\\{unread_count}", Matcher.quoteReplacement(String.valueOf(database().countMails(user, TriState.FALSE))))
                            .replaceAll("\\{total_count}", Matcher.quoteReplacement(String.valueOf(database().countMails(user, TriState.NOT_SET))))
                            .replaceAll("\\{page}", Matcher.quoteReplacement(String.valueOf(menu.page)))
                            .replaceAll("\\{total_pages}", Matcher.quoteReplacement(String.valueOf((int) Math.ceil(database().countMails(user, TriState.NOT_SET) / 27.0))))
                            .replaceAll("\\{max_mail_count}", Matcher.quoteReplacement(String.valueOf(config().mail.maxMailCount)))
                            .replaceAll("\\{player_name}", Matcher.quoteReplacement(viewer.getName()))
            );
        });
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack leftArrow = Bukkit.getItemFactory().createItemStack(config().leftArrowItem);
        leftArrow.editMeta(meta -> meta.itemName(plainText().deserialize(messages().previousPage)));
        if (page > 1) clickRegistry.setItem(0, leftArrow, (_, clickType) -> {
            if (clickType.isLeftClick() && page > 1) {
                page--;
                refresh();
            }
        });
        ItemStack rightArrow = Bukkit.getItemFactory().createItemStack(config().rightArrowItem);
        rightArrow.editMeta(meta -> meta.itemName(plainText().deserialize(messages().nextPage)));
        if (database().countMails(owner, TriState.NOT_SET) > page * 27) {
            clickRegistry.setItem(8, rightArrow, (_, clickType) -> {
                if (clickType.isLeftClick() && database().countMails(owner, TriState.NOT_SET) > page * 27) {
                    page++;
                    refresh();
                }
            });
        }
        ItemStack refreshItem = ItemStack.of(Material.WIND_CHARGE);
        refreshItem.editMeta(meta -> meta.itemName(plainText().deserialize(messages().refreshButton)));
        refreshItem.editMeta(meta -> {
            var cooldown = meta.getUseCooldown();
            cooldown.setCooldownGroup(MAIL_INBOX_KEY);
            meta.setUseCooldown(cooldown);
        });
        if (System.currentTimeMillis() <= this.cooldown)
            viewer.setCooldown(MAIL_INBOX_KEY, (int) ((this.cooldown - System.currentTimeMillis()) / 50));
        clickRegistry.setItem(4, refreshItem, (_, clickType) -> {
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
        database().deleteAllUserNotification(owner);
        for (Mail mail : database().getMails(owner, TriState.NOT_SET, page).stream().sorted().toList().reversed()) {
            clickRegistry.setItem(slot, createMailItem(mail), (_, clickType) -> {
                if (clickType.isLeftClick()) {
//                    ThreadUtils.runSync(viewer, () -> new MailDialogView(viewer, this, mail, owner).open());
                    openMenu(new MailViewerMenu(viewer, owner, mail, true));
                } else if (clickType.isRightClick() && mail.getAttachmentsInternal().stream().allMatch(a -> a.opened() || a.isExpired())) {
                    if (clickType.isShiftClick()) {
                        database().deleteMail(mail);
                        refresh();
                        return;
                    }
                    new ConfirmMenu(
                            viewer,
                            this,
                            miniMessage().deserialize(messages().deleteMailConfirmTitle),
                            miniMessage().deserialize(
                                    messages().deleteMailConfirmContent,
                                    Placeholder.component("mail_title", plainText().deserialize(mail.getTitle()))
                            ),
                            ok -> {
                                if (ok) database().deleteMail(mail);
                                refresh();
                            }).open();
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
            List<Component> lore = messages().mailMenuMailLore
                    .replaceAll("\\{sender}", Matcher.quoteReplacement(mail.getSender().name()))
                   .replaceAll("\\{time}", Matcher.quoteReplacement(DateUtils.format(mail.getSentTime())))
                    .replaceAll("\\{attachments}", Matcher.quoteReplacement(mail.attachments().size() + " (" + messages().unreceived + " " + mail.getAttachmentsInternal().stream().filter(a -> !a.opened() && !a.isExpired()).count() + ")"))
                   .replaceAll("\\{read}", Matcher.quoteReplacement(mail.isRead() ? messages().read : messages().unread))
                    .transform(s -> Stream.of(s.split("\n")))
                    .filter(s -> !s.isBlank()).map(miniMessage()::deserialize)
                    .collect(Collectors.toCollection(ArrayList::new));
            lore.addFirst(miniMessage().deserialize(messages().rightClickTo.replace("{action}", messages().clickActionDelete)));
            lore.addFirst(miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionOpen)));
            meta.lore(lore);
        });
        return item;
    }

}
