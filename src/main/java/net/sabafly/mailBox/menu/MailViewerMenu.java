package net.sabafly.mailBox.menu;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class MailViewerMenu extends BaseMenu<MailViewerMenu> {

    private final Mail mail;
    private final boolean openPreviousMenu;

    public MailViewerMenu(Player player, Mail mail, boolean openPreviousMenu) {
        super(player, getSlot(mail.getAttachments().size()), miniMessage().deserialize(config().messages.mailViewerMenuTitle));
        this.mail = mail;
        this.openPreviousMenu = openPreviousMenu;
    }

    private static int getSlot(int attachments) {
        if (attachments <= 5) return 9;
        if (attachments <= 9) return 27;
        if (attachments <= 18) return 36;
        return 45;
    }

    @Override
    public void open() {
        mail.setRead(true);
        database().updateMail(mail);
        super.open();
    }

    @Override
    protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
        if (openPreviousMenu) {
            setNextMenu(new MailMenu(player));
        }
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        final ItemStack senderItem = getSenderItem();
        clickRegistry.setItem(0, senderItem);
        ItemStack titleItem = new ItemStack(Material.NAME_TAG);
        titleItem.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.title, TagResolver.builder().tag("title", Tag.inserting(plainText().deserialize(mail.getTitle()))).build())));
        clickRegistry.setItem(1, titleItem);
        ItemStack contentItem = new ItemStack(Material.BOOK);
        contentItem.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.content));
            meta.lore(List.of(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionRead))));
        });
        clickRegistry.setItem(2, contentItem, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new OpenBookMenu(p, Book.builder()
                        .author(mail.getSender() == null ? miniMessage().deserialize(config().messages.systemName) : p.name())
                        .pages(Arrays.stream(mail.getContent().split("§"))
                                .map(plainText()::deserialize)
                                .collect(Collectors.toUnmodifiableList())
                        ).build()));
            }
        });
        ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        int start;
        int count;
        if (mail.getAttachments().isEmpty()) {
            start = 3;
            count = 6;
        } else if (mail.getAttachments().size() <= 5) {
            start = 3;
            count = 1;
        } else {
            start = 3;
            count = 15;
        }
        for (int i = 0; i < count; i++) {
            clickRegistry.setItem(start + i, glassPane);
        }
        for (int i = 0; i < mail.getAttachments().size(); i++) {
            int finalI = i;
            clickRegistry.setItem(start + count + i, mail.getAttachments().get(i).getPreview(attachment->{
                List<Component> lore = config().messages.attachmentLore
                        .replace("{received}", attachment.received() ? config().messages.received : attachment.isExpired() ? config().messages.expired : config().messages.notReceived)
                        .replace("{expires}", attachment.getExpireTime().map(LocalDateTime::toString).orElse(config().messages.expiresNever))
                        .transform(s -> Stream.of(s.split("\n")))
                        .filter(str -> !str.isBlank()).map(miniMessage()::deserialize).collect(Collectors.toList());
                lore.addFirst(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionReceive)));
                return lore;
            }), (p, clickType) -> {
                final Attachment<?> attachment = mail.getAttachments().get(finalI);
                if (clickType.isLeftClick() && !attachment.received() && !attachment.isExpired()) {
                    attachment.apply(p);
                    attachment.setReceived(true);
                    database().updateAttachment(attachment);
                    refresh();
                }
            });
        }
    }

    private @NotNull ItemStack getSenderItem() {
        ItemStack senderItem = new ItemStack(Material.PLAYER_HEAD);
        senderItem.editMeta(meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setPlayerProfile(mail.getSender() == null ? null : Bukkit.getOfflinePlayer(mail.getSender().uuid()).getPlayerProfile());
            }
        });
        senderItem.editMeta(meta ->
                meta.customName(miniMessage().deserialize(config().messages.sender, TagResolver.builder().tag("sender", Tag.inserting(plainText().deserialize(mail.getSender() == null ? config().messages.systemName : Optional.ofNullable(Bukkit.getOfflinePlayer(mail.getSender().uuid()).getName()).orElse(mail.getSender().uuid().toString())))).build())));
        return senderItem;
    }
}
