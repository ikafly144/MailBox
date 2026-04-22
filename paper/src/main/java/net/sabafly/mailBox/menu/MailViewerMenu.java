package net.sabafly.mailBox.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.commands.arguments.MailUserArgumentType;
import net.sabafly.mailBox.mail.IAttachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class MailViewerMenu extends InventoryMenu<MailViewerMenu> {

    private final Mail mail;
    private final boolean openPreviousMenu;
    private final User owner;

    public MailViewerMenu(Player viewer, User owner, Mail mail, boolean openPreviousMenu) {
        super(viewer, getSlot(mail.attachments().size()), miniMessage().deserialize(config().messages.mailViewerMenuTitle));
        this.owner = owner;
        this.mail = mail;
        this.openPreviousMenu = openPreviousMenu;
    }

    private static int getSlot(int attachments) {
        if (attachments <= 5) return 9;
        if (attachments <= 9) return 27;
        if (attachments <= 18) return 36;
        return 45;
    }

    private boolean isOwnerView() {
        return owner.id().equals(viewer.identity().uuid());
    }

    @Override
    public void open() {
        var read = mail.isRead();
        if (isOwnerView()) mail.setRead(true);
        database().updateMail(mail);
        super.open();
        if (!read && isOwnerView())
            openMenu(new ContentMenu(viewer, miniMessage().deserialize(mail.getTitle()), miniMessage().deserialize(mail.getContent()), this));
    }

    @Override
    protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
        if (openPreviousMenu) {
            setNextMenu(new InboxMenu(player, owner));
        }
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        final var replyEnabled = !mail.sender().id().equals(viewer.identity().uuid()) && MailUserArgumentType.checkPermission(viewer, mail.sender().key());
        final ItemStack senderItem = getSenderItem(replyEnabled);

        clickRegistry.setItem(0, senderItem, (p, clickType) -> {
            if (replyEnabled && clickType.isLeftClick()) {
                openMenu(new CreateMailMenu(p, mail.sender(), this));
            }
        });
        ItemStack titleItem = new ItemStack(Material.NAME_TAG);
        titleItem.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.subjectValue, TagResolver.builder().tag("subject", Tag.inserting(plainText().deserialize(mail.getTitle()))).build())));
        clickRegistry.setItem(1, titleItem);
        ItemStack contentItem = new ItemStack(Material.BOOK);
        contentItem.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.content));
            meta.lore(List.of(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionRead))));
        });
        clickRegistry.setItem(2, contentItem, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new ContentMenu(p, miniMessage().deserialize(mail.getTitle()), miniMessage().deserialize(mail.getContent()), this));
            }
        });
        ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        glassPane.editMeta(meta -> meta.setHideTooltip(true));
        int start;
        int count;
        if (mail.attachments().isEmpty()) {
            start = 3;
            count = 6;
        } else if (mail.attachments().size() <= 5) {
            start = 3;
            count = 1;
        } else {
            start = 3;
            count = 15;
        }
        for (int i = 0; i < count; i++) {
            clickRegistry.setItem(start + i, glassPane);
        }
        for (int i = 0; i < mail.attachments().size(); i++) {
            int finalI = i;
            clickRegistry.setItem(start + count + i, mail.getAttachmentsInternal().get(i).createPreview(attachment -> {
                List<Component> lore = config().messages.attachmentLore
                        .replaceAll("\\{received}", attachment.opened() ? config().messages.received : attachment.isExpired() ? config().messages.expired : config().messages.notReceived)
                        .replaceAll("\\{expires}", attachment.expireTime().map(t -> t.format(DateTimeFormatter.ofPattern(config().mail.dateFormat))).orElse(config().messages.expiresNever))
                        .transform(s -> Stream.of(s.split("\n")))
                        .filter(str -> !str.isBlank()).map(miniMessage()::deserialize).collect(Collectors.toList());
                lore.addFirst(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionReceive)));
                return lore;
            }), (p, clickType) -> {
                final IAttachment<?, ?> attachment = mail.getAttachmentsInternal().get(finalI);
                if (clickType.isLeftClick()) {
                    if (attachment.canOpen()) {
                        attachment.apply(p);
                        attachment.setOpened(true);
                        database().updateMailAttachment(mail, attachment);
                        refresh();
                    } else {
                        p.sendMessage(miniMessage().deserialize(
                                config().messages.attachmentCannotOpen,
                                Placeholder.component("attachment", attachment.getName()),
                                Placeholder.parsed("reason", attachment.isExpired() ? config().messages.attachmentExpired :
                                        attachment.opened() ? config().messages.attachmentAlreadyReceived :
                                        "unknown")
                        ));
                    }
                }
            });
        }
    }

    private @NotNull ItemStack getSenderItem(boolean replyEnabled) {
        ItemStack senderItem = new ItemStack(Material.PLAYER_HEAD);

        senderItem.editMeta(meta -> {
            if (meta instanceof SkullMeta skullMeta) {
                try {
                    var profile = Bukkit.createProfile(mail.getSender().id(), mail.getSender().name());
                    var texture = profile.getTextures();
                    texture.setSkin(mail.getSender().skinUrl());
                    profile.setTextures(texture);
                    skullMeta.setPlayerProfile(profile);
                } catch (IllegalArgumentException ignored) {
                }
            }
        });
        senderItem.editMeta(meta ->
                {
                    meta.customName(miniMessage().deserialize(
                            config().messages.senderValue,
                            TagResolver.builder().tag(
                                    "sender",
                                    Tag.inserting(
                                            miniMessage().deserialize(
                                                    mail.getSender().name()
                                            )
                                    )
                            ).build()
                    ));
                    if (replyEnabled) {
                        meta.lore(List.of(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionReply))));
                    }
                }
        );
        return senderItem;
    }
}
