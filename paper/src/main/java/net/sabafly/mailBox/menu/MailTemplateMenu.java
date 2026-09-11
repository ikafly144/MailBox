package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.utils.DateUtils;
import net.sabafly.mailbox.api.mail.User;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.messages;
import static net.sabafly.mailBox.MailBox.database;

public class MailTemplateMenu extends InventoryMenu<MailTemplateMenu> {

    private int page;

    public MailTemplateMenu(Player player, int page) {
        super(player, 45, menu -> miniMessage().deserialize(messages().mailTemplateMenuTitle + " " + menu.page));
        this.page = page;
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack arrow = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.ARROW).createItemStack();
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(messages().previousPage)));
        clickRegistry.setItem(0, arrow, (_, clickType) -> {
            if (clickType.isLeftClick() && page > 1) {
                page--;
                refresh();
            }
        });
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(messages().nextPage)));
        clickRegistry.setItem(8, arrow, (_, clickType) -> {
            if (clickType.isLeftClick()) {
                page++;
                refresh();
            }
        });
        ItemStack paper = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.PAPER).createItemStack();
        paper.editMeta(meta -> meta.itemName(plainText().deserialize(messages().createMailTemplate)));
        clickRegistry.setItem(4, paper, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new CreateMailMenu(player, this));
            }
        });
        ItemStack glassPane = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.BLACK_STAINED_GLASS_PANE).createItemStack();
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < 9; i++) {
            clickRegistry.setItem(9 + i, glassPane);
        }
        int slot = 18;
        for (MailTemplate mail : database().getMailTemplates(page).stream().sorted().toList()) {
            clickRegistry.setItem(slot, createMailItem(mail), (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    openMenu(new MailTemplateEditMenu(p, mail));
                }
            });
            slot++;
        }
    }

    private ItemStack createMailItem(MailTemplate mail) {
        ItemStack item = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.WRITABLE_BOOK).createItemStack();
        item.editMeta(meta -> {
            meta.itemName(plainText().deserialize(mail.subject()));
            if (mail.autoSend()) {
                meta.addEnchant(Enchantment.INFINITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(messages().mailTemplateLore
                   .replaceAll("\\{sender}", Matcher.quoteReplacement(Optional.of(mail.sender()).map(User::name).orElse(messages().systemName)))
                   .replaceAll("\\{start}", Matcher.quoteReplacement(Optional.ofNullable(mail.startTime()).map(DateUtils::format).orElse(messages().noValue)))
                   .replaceAll("\\{end}", Matcher.quoteReplacement(Optional.ofNullable(mail.endTime()).map(DateUtils::format).orElse(messages().noValue)))
                   .replaceAll("\\{interval}", Matcher.quoteReplacement(Optional.ofNullable(mail.interval()).map(d -> DurationFormatUtils.formatDuration(d.toMillis(), "HH:mm:ss")).orElse(messages().noValue)))
                   .replaceAll("\\{attachments}", Matcher.quoteReplacement(mail.attachment().size() + ""))
                   .replaceAll("\\{auto_send}", Matcher.quoteReplacement(mail.autoSend() ? messages().enabled : messages().disabled))
                    .transform(s -> List.of(s.split("\n")))
                    .stream().filter(s -> !s.isBlank()).map(miniMessage()::deserialize).toList());
        });
        return item;
    }
}
