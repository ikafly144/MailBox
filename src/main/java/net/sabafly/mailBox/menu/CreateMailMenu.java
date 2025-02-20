package net.sabafly.mailBox.menu;

import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class CreateMailMenu extends BaseMenu<CreateMailMenu> {

    @Nullable
    private String title = null;
    @Nullable
    private ItemStack content = null;
    @NotNull
    private List<@NotNull Attachment<?>> attachments = new ArrayList<>();
    private boolean created = false;
    @Nullable
    private final OfflinePlayer target;
    @Nullable
    private BaseMenu<?> nextMenu = null;

    public CreateMailMenu(@NotNull Player player, @Nullable BaseMenu<?> nextMenu) {
        this(player, (OfflinePlayer) null);
        this.nextMenu = nextMenu;
    }

    public CreateMailMenu(@NotNull Player player, @Nullable OfflinePlayer target) {
        super(player, 9, miniMessage().deserialize(config().messages.createMailMenuTitle));
        this.target = target;
    }

    @Override
    protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
        if (getNextMenu() != null) return;
        if (!created && content != null) {
            player.getInventory().addItem(content).forEach((i, s) -> player.getWorld().dropItem(player.getLocation(), s));
        }
        if (!created && !attachments.isEmpty()) {
            attachments.forEach(a -> a.cancel(player));
        }
        if (nextMenu != null) {
            setNextMenu(nextMenu);
        }
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack titleItem = new ItemStack(Material.NAME_TAG);
        titleItem.editMeta(meta -> meta.itemName(title == null ?
                miniMessage().deserialize(config().messages.setTitle) :
                miniMessage().deserialize(config().messages.title, TagResolver.builder().tag("title", Tag.inserting(plainText().deserialize(title))).build())
        ));
        clickRegistry.setItem(0, titleItem, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new AnvilSetterMenu(this, player, miniMessage().deserialize(config().messages.inputMenuTitle), s -> title = s));
            }
        });
        ItemStack contentItem = new ItemStack(Material.WRITABLE_BOOK);
        contentItem.editMeta(meta -> meta.itemName(content == null ?
                miniMessage().deserialize(config().messages.setContent) :
                miniMessage().deserialize(config().messages.contentBook, TagResolver.builder().tag("title", Tag.inserting(content.effectiveName())).build())
        ));
        clickRegistry.setItem(1, contentItem, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new BookMenu(this, player, content, item -> {
                    content = item;
                    if (item != null && item.getItemMeta() instanceof BookMeta bookMeta) {
                        Optional.ofNullable(bookMeta.title())
                                .map(plainText()::serialize)
                                .ifPresent(t -> title = t);
                    }
                }));
            }
        });
        ItemStack attachmentItem = new ItemStack(Material.CHEST);
        attachmentItem.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.setAttachment)));
        clickRegistry.setItem(2, attachmentItem, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new AttachmentMenu(this, p, attachments, attachments -> this.attachments = attachments));
            }
        });
        if (target == null) {
            ItemStack createTemplate = new ItemStack(Material.WRITABLE_BOOK);
            createTemplate.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.createMailTemplate)));
            clickRegistry.setItem(8, createTemplate, (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    if (title == null || content == null) {
                        MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> p.sendMessage(miniMessage().deserialize(config().messages.createMailTemplateError))));
                        return;
                    }
                    MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> {
                        String contentString = String.join("§", ((WritableBookMeta) content.getItemMeta()).getPages());
                        MailTemplate template = MailTemplate.createNow(null, title, contentString, attachments);
                        database().createMailTemplate(template);
                        p.sendMessage(miniMessage().deserialize(config().messages.createMailTemplateSuccess));
                    }));
                    created = true;
                    p.closeInventory();
                }
            });
        } else {
            ItemStack sendItem = new ItemStack(Material.GREEN_WOOL);
            sendItem.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.send)));
            clickRegistry.setItem(8, sendItem, (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    if (title == null || content == null) {
                        MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> p.sendMessage(miniMessage().deserialize(config().messages.createMailError))));
                        return;
                    }
                    if (database().countMails(database().getUser(p.getUniqueId()),TriState.NOT_SET)>=config().mail.maxMailCount) {
                        MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> p.sendMessage(miniMessage().deserialize(config().messages.mailBoxFull))));
                        return;
                    }
                    MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> {
                        String contentString = String.join("§", ((WritableBookMeta) content.getItemMeta()).getPages());
                        Mail mail = Mail.createNow(p, target, title, contentString, attachments);
                        database().createMail(mail);
                        p.sendMessage(miniMessage().deserialize(config().messages.createMailSuccess));
                    }));
                    created = true;
                    p.closeInventory();
                }
            });
        }

    }

    public static class BookMenu extends BaseMenu<BookMenu> {

        private final CreateMailMenu menu;
        private final Consumer<ItemStack> consumer;
        private final ItemStack item;

        public BookMenu(CreateMailMenu menu, Player player, @Nullable ItemStack item, Consumer<@Nullable ItemStack> consumer) {
            super(player, InventoryType.DROPPER, miniMessage().deserialize(config().messages.bookMenuTitle), true);
            this.menu = menu;
            this.consumer = consumer;
            this.item = item;
        }

        @Override
        protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
            ItemStack item = inventory.getTopInventory().getItem(4);
            if (item == null || item.getType() == Material.WRITABLE_BOOK) {
                consumer.accept(item);
            } else if (item.getType() == Material.WRITTEN_BOOK) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().addItem(item).forEach((i, s) -> player.getWorld().dropItem(player.getLocation(), s));
                    item.setAmount(1);
                }
                consumer.accept(item);
            } else if (this.item == null) {
                player.getInventory().addItem(item).forEach((i, s) -> player.getWorld().dropItem(player.getLocation(), s));
            }
            setNextMenu(menu);
        }

        @Override
        void setItems(@NotNull ClickRegistry clickRegistry) {
            ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            glassPane.editMeta(meta -> meta.setHideTooltip(true));
            for (int i = 0; i < 9; i++) {
                if (i == 4 && item != null) {
                    clickRegistry.setItem(i, item);
                } else if (i != 4) {
                    clickRegistry.setItem(i, glassPane, (player1, clickType) -> {
                    });
                }
            }
        }
    }

    public static class AttachmentMenu extends BaseMenu<AttachmentMenu> {

        private final CreateMailMenu menu;
        private final Consumer<List<@NotNull Attachment<?>>> consumer;
        private final List<@NotNull Attachment<?>> attachments;

        public AttachmentMenu(CreateMailMenu menu, Player player, @NotNull List<@NotNull Attachment<?>> attachments, Consumer<List<@NotNull Attachment<?>>> consumer) {
            super(player, 45, miniMessage().deserialize(config().messages.attachmentMenuTitle));
            this.menu = menu;
            this.consumer = consumer;
            this.attachments = attachments;
        }

        @Override
        protected void onClose(@NotNull Player player, @NotNull InventoryView inventory) {
            consumer.accept(attachments);
            setNextMenu(menu);
        }

        @Override
        void setItems(@NotNull ClickRegistry clickRegistry) {
            if (player.hasPermission("mailbox.attachment.item")) {
                ItemStack chest = new ItemStack(Material.CHEST);
                chest.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.attachmentAppendItem)));
                clickRegistry.setItem(0, chest, (p, clickType) -> {
                    if (clickType.isLeftClick() && (p.hasPermission("mailbox.attachment.admin") || attachments.size() < config().mail.maxAttachmentCount)) {
                        openMenu(new AttachmentItemMenu(this, p, attachment -> {
                            if (attachment != null) {
                                attachments.add(attachment);
                            }
                        }));
                    }
                });
            }
            if (player.hasPermission("mailbox.attachment.command")) {
                ItemStack commandBlock = new ItemStack(Material.COMMAND_BLOCK);
                commandBlock.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.attachmentAppendCommand)));
                clickRegistry.setItem(1, commandBlock, (p, clickType) -> {
                    if (clickType.isLeftClick() && (p.hasPermission("mailbox.attachment.admin") || attachments.size() < config().mail.maxAttachmentCount)) {
                        openMenu(new AttachmentCommandMenu(this, p, attachment -> {
                            if (attachment != null) {
                                attachments.add(attachment);
                            }
                        }));
                    }
                });
            }

            ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            glassPane.editMeta(meta -> meta.setHideTooltip(true));
            if (size() > 9) {
                for (int i = 0; i < 9; i++) {
                    clickRegistry.setItem(i + 9, glassPane);
                }
                for (int i = 0; i < attachments.size(); i++) {
                    int finalI = i;
                    clickRegistry.setItem(i + 18, attachments.get(i).getPreview(menu -> List.of(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionDelete)))), (player1, clickType) -> {
                        if (clickType.isLeftClick()) {
                            attachments.remove(finalI).cancel(player1);
                            refresh();
                        }
                    });
                }
            }
        }
    }

}
