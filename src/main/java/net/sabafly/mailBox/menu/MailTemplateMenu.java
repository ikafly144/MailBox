package net.sabafly.mailBox.menu;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import net.sabafly.mailBox.mail.MailTemplate;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

@SuppressWarnings("UnstableApiUsage")
public class MailTemplateMenu extends BaseMenu<MailTemplateMenu> {

    private int page;

    public MailTemplateMenu(Player player, int page) {
        super(player, 45, menu -> miniMessage().deserialize(config().messages.mailTemplateMenuTitle + " " + menu.page));
        this.page = page;
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack arrow = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.ARROW).createItemStack();
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.previousPage)));
        clickRegistry.setItem(0, arrow, (player, clickType) -> {
            if (clickType.isLeftClick() && page > 1) {
                page--;
                refresh();
            }
        });
        arrow.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.nextPage)));
        clickRegistry.setItem(8, arrow, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                page++;
                refresh();
            }
        });
        ItemStack paper = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(ItemTypeKeys.PAPER).createItemStack();
        paper.editMeta(meta -> meta.itemName(plainText().deserialize(config().messages.createMailTemplate)));
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
            meta.itemName(plainText().deserialize(mail.title()));
            if (mail.autoSend()) {
                meta.addEnchant(Enchantment.INFINITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.lore(config().messages.mailTemplateLore
                    .replace("{sender}", Optional.ofNullable(mail.sender()).map(sender -> Bukkit.getOfflinePlayer(sender.uuid()).getName()).orElse(config().messages.systemName))
                    .replace("{start}", Optional.ofNullable(mail.startTime()).map(LocalDateTime::toString).orElse(config().messages.noValue))
                    .replace("{end}", Optional.ofNullable(mail.endTime()).map(LocalDateTime::toString).orElse(config().messages.noValue))
                    .replace("{interval}", Optional.ofNullable(mail.interval()).map(d -> DurationFormatUtils.formatDuration(d.toMillis(), "HH:mm:ss")).orElse(config().messages.noValue))
                    .replace("{attachments}", mail.attachment().size() + "")
                    .replace("{auto_send}", mail.autoSend() ? config().messages.enabled : config().messages.disabled)
                    .transform(s -> List.of(s.split("\n")))
                    .stream().filter(s -> !s.isBlank()).map(miniMessage()::deserialize).toList());
        });
        return item;
    }
}
