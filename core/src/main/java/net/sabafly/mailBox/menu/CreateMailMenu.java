package net.sabafly.mailBox.menu;

import io.papermc.paper.configuration.type.Duration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.attachments.VaultValueAttachment;
import net.sabafly.mailBox.utils.EconomyUtils;
import net.sabafly.mailBox.utils.ThreadUtils;
import net.sabafly.mailbox.api.mail.User;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class CreateMailMenu extends InventoryMenu<CreateMailMenu> {

    @Nullable
    private String subject = null;
    @Nullable
    private String content = null;
    @NotNull
    private List<@NotNull Attachment<?>> attachments = new ArrayList<>();
    private boolean created = false;
    @Nullable
    private final User target;
    @Nullable
    private final InventoryMenu<?> nextMenu;

    public CreateMailMenu(@NotNull Player player, @Nullable InventoryMenu<?> nextMenu) {
        this(player, null, nextMenu);
    }

    public CreateMailMenu(@NotNull Player player, @Nullable User target) {
        this(player, target, null);
    }

    public CreateMailMenu(@NotNull Player player, @Nullable User target, @Nullable InventoryMenu<?> nextMenu) {
        this.target = target;
        this.nextMenu = nextMenu;
        super(player, 9, miniMessage().deserialize(config().messages.createMailMenuTitle));
    }

    @Override
    protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
        if (getNextMenu() != null) return;
        if (!created && !attachments.isEmpty()) {
            attachments.forEach(a -> a.cancel(player));
        }
        if (nextMenu != null) {
            setNextMenu(nextMenu);
        }
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack subjectItem = new ItemStack(Material.NAME_TAG);
        subjectItem.editMeta(meta -> meta.itemName(subject == null ?
                miniMessage().deserialize(config().messages.setSubject) :
                miniMessage().deserialize(config().messages.subjectValue, TagResolver.builder().tag("subject", Tag.inserting(plainText().deserialize(subject))).build())
        ));
        clickRegistry.setItem(0, subjectItem, (_, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, viewer, miniMessage().deserialize(config().messages.setSubject), s -> subject = s, subject, false, 50));
            }
        });
        ItemStack contentItem = new ItemStack(Material.WRITABLE_BOOK);
        contentItem.editMeta(meta -> meta.itemName(content == null ?
                miniMessage().deserialize(config().messages.setContent) :
                miniMessage().deserialize(config().messages.contentInfo, TagResolver.builder().tag("length", Tag.inserting(Component.text(content.length()))).build())
        ));
        clickRegistry.setItem(1, contentItem, (_, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, viewer, miniMessage().deserialize(config().messages.setContent), str -> this.content = str, this.content, true, 2000));
            }
        });
        ItemStack attachmentItem = new ItemStack(Material.CHEST);
        attachmentItem.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.setAttachment)));
        clickRegistry.setItem(2, attachmentItem, (p, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new AttachmentMenu(this, p, attachments, attachments -> this.attachments = attachments, target == null));
            }
        });
        // NOTE: When template mode
        if (target == null) {
            ItemStack createTemplate = new ItemStack(Material.WRITABLE_BOOK);
            createTemplate.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.createMailTemplate)));
            clickRegistry.setItem(8, createTemplate, (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    if (subject == null || content == null) {
                        MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> p.sendMessage(miniMessage().deserialize(config().messages.createMailTemplateError))));
                        return;
                    }
                    MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> {
                        MailTemplate template = MailTemplate.createNow(MailBox.getInstance().getSystemUser(), subject, content, attachments);
                        database().createMailTemplate(template);
                        p.sendMessage(miniMessage().deserialize(config().messages.createMailTemplateSuccess));
                    }));
                    created = true;
                    p.closeInventory();
                }
            });
        } else {
            ItemStack sendItem = new ItemStack(Material.GREEN_WOOL);
            sendItem.editMeta(meta -> {
                meta.itemName(miniMessage().deserialize(config().messages.send));
                if (config().mail.mailPrice > 0 || config().mail.attachmentPrice > 0) {
                    int totalPrice = config().mail.mailPrice + attachments.size() * config().mail.attachmentPrice;
                    List<Component> lore = new ArrayList<>();
                    if (config().mail.mailPrice > 0) {
                        lore.add(miniMessage().deserialize(
                                config().messages.mailPriceInfo,
                                Placeholder.component("price", Component.text(config().mail.mailPrice)),
                                Placeholder.component("currency", Component.text(EconomyUtils.getEconomy().currencyNamePlural()))
                        ));
                    }
                    if (config().mail.attachmentPrice > 0) {
                        lore.add(miniMessage().deserialize(
                                config().messages.attachmentPriceInfo,
                                Placeholder.component("price", Component.text(config().mail.attachmentPrice)),
                                Placeholder.component("count", Component.text(attachments.size())),
                                Placeholder.component("total_price", Component.text(totalPrice)),
                                Placeholder.component("currency", Component.text(EconomyUtils.getEconomy().currencyNamePlural()))
                        ));
                        lore.add(miniMessage().deserialize(
                                config().messages.totalPriceInfo,
                                Placeholder.component("price", Component.text(totalPrice)),
                                Placeholder.component("currency", Component.text(EconomyUtils.getEconomy().currencyNamePlural()))
                        ));
                    }
                    meta.lore(lore);
                }
            });
            clickRegistry.setItem(8, sendItem, (p, clickType) -> {
                if (clickType.isLeftClick()) {
                    if (subject == null || content == null) {
                        MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> p.sendMessage(miniMessage().deserialize(config().messages.createMailError))));
                        return;
                    }
                    var playerUser = database().getUser(viewer.getUniqueId());
                    if (playerUser == null) {
                        throw new IllegalStateException("Player user not found");
                    }
                    if (database().countMails(target, TriState.NOT_SET) >= config().mail.maxMailCount) {
                        MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> p.sendMessage(miniMessage().deserialize(config().messages.mailBoxFull))));
                        return;
                    }
                    if (config().mail.mailPrice > 0 || config().mail.attachmentPrice > 0) {
                        int totalPrice = config().mail.mailPrice + attachments.size() * config().mail.attachmentPrice;
                        if (totalPrice > 0 &&!EconomyUtils.getEconomy().withdrawPlayer(p, totalPrice).transactionSuccess()) {
                            p.sendMessage(miniMessage().
                                    deserialize(
                                            config().messages.notEnoughMoney,
                                            Placeholder.component("currency", Component.text(EconomyUtils.getEconomy().currencyNamePlural())),
                                            Placeholder.component("price", Component.text(totalPrice)),
                                            Placeholder.component("missing_amount", Component.text(new DecimalFormat("#.##########").format(totalPrice - EconomyUtils.getEconomy().getBalance(p)))),
                                            Placeholder.component("mail_price", Component.text(config().mail.mailPrice)),
                                            Placeholder.component("attachment_price", Component.text(config().mail.attachmentPrice))
                                    ));
                            return;
                        }
                    }
                    MailBox.getThreadedQueue().submit(() -> ThreadUtils.runSync(() -> {
                        Mail mail = Mail.createFromUserNow(playerUser, target, subject, content, attachments);
                        database().createMail(mail);
                        p.sendMessage(miniMessage().deserialize(config().messages.createMailSuccess));
                    }));
                    created = true;
                    p.closeInventory();
                }
            });
        }

    }

    public static class AttachmentMenu extends InventoryMenu<AttachmentMenu> {

        private final Menu menu;
        private final Consumer<List<@NotNull Attachment<?>>> consumer;
        private final List<@NotNull Attachment<?>> attachments;

        private final boolean isTemplate;

        public AttachmentMenu(Menu menu, Player player, @NotNull List<@NotNull Attachment<?>> attachments, Consumer<List<@NotNull Attachment<?>>> consumer, boolean isTemplate) {
            super(player, 45, miniMessage().deserialize(config().messages.attachmentMenuTitle));
            this.menu = menu;
            this.consumer = consumer;
            this.attachments = attachments;
            this.isTemplate = isTemplate;
        }

        private void addAttachment(Attachment<?> attachment) {
            if (attachment != null) {
                if (!isTemplate) attachment.expireDuration(config().mail.getExpirationDuration());
                attachments.add(attachment);
            }
        }

        @Override
        protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
            consumer.accept(attachments);
            setNextMenu(menu);
        }

        @Override
        void setItems(@NotNull ClickRegistry clickRegistry) {
            if (viewer.hasPermission("mailbox.attachment.item")) {
                ItemStack chest = new ItemStack(Material.CHEST);
                chest.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.attachmentAppendItem)));
                clickRegistry.setItem(0, chest, (p, clickType) -> {
                    if (clickType.isLeftClick() && (p.hasPermission("mailbox.attachment.admin") || attachments.size() < config().mail.maxAttachmentCount)) {
                        openMenu(new AttachmentItemMenu(this, p, attachment -> {
                            if (attachment != null) {
                                addAttachment(attachment);
                            }
                        }));
                    }
                });
            }
            if (viewer.hasPermission("mailbox.attachment.command")) {
                ItemStack commandBlock = new ItemStack(Material.COMMAND_BLOCK);
                commandBlock.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.attachmentAppendCommand)));
                clickRegistry.setItem(1, commandBlock, (p, clickType) -> {
                    if (clickType.isLeftClick() && (p.hasPermission("mailbox.attachment.admin") || attachments.size() < config().mail.maxAttachmentCount)) {
                        openMenu(new AttachmentCommandMenu(this, p, attachment -> {
                            if (attachment != null) {
                                addAttachment(attachment);
                            }
                        }));
                    }
                });
            }
            if (viewer.hasPermission("mailbox.attachment.vault") && MailBox.isVaultEnabled()) {
                ItemStack emerald = new ItemStack(Material.PAPER);
                emerald.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.attachmentAppendVault
                        .replaceAll("\\{currency}", Matcher.quoteReplacement(EconomyUtils.getEconomy().currencyNamePlural()))
                )));
                clickRegistry.setItem(2, emerald, (p, clickType) -> {
                    if (clickType.isLeftClick() && (p.hasPermission("mailbox.attachment.admin") || attachments.size() < config().mail.maxAttachmentCount)) {
                        openMenu(new AttachmentVaultValueMenu(this, p, attachment -> {
                            if (!isTemplate && !EconomyUtils.getEconomy().withdrawPlayer(viewer, attachment.value()).transactionSuccess())
                                return;
                            if (attachment != null) {
                                addAttachment(attachment);
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
                    final int finalI = i;
                    List<Component> lore = new ArrayList<>(List.of(
                            miniMessage().deserialize(config().messages.expirationValue, TagResolver.builder().tag(
                                    "expiration", Tag.inserting(miniMessage().deserialize(attachments.get(i).expireDuration().map(d -> DurationFormatUtils.formatDuration(d.toMillis(), "HH:mm:ss")).orElse(config().messages.expiresNever)))
                            ).build())
                    ));
                    if (isTemplate) {
                        lore.add(miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSetExpiration)));
                    }
                    lore.add(miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionDelete)));
                    clickRegistry.setItem(i + 18, attachments.get(i).createPreview(_ -> lore), (player1, clickType) -> {
                        if (clickType.isRightClick()) {
                            var a = attachments.remove(finalI);
                            if (!isTemplate || !(a instanceof VaultValueAttachment)) a.cancel(player1);
                            refresh();
                        } else if (clickType.isLeftClick() && isTemplate) {
                            openMenu(new StringInputMenu(this, player1, miniMessage().deserialize(config().messages.setExpiration), s -> {
                                try {
                                    final long seconds = Duration.of(s).seconds();
                                    if (seconds <= 0) {
                                        attachments.get(finalI).expireDuration(null);
                                    } else {
                                        attachments.get(finalI).expireDuration(java.time.Duration.ofSeconds(seconds));
                                    }
                                } catch (Exception e) {
                                    MailBox.logger().error("Error while setting expiration", e);
                                }
                            }, attachments.get(finalI).expireDuration().map(d -> io.papermc.paper.configuration.type.Duration.of("%d%s".formatted(d.toMinutes() == 0 ? d.toSeconds() : d.toHours() == 0 ? d.toMinutes() : d.toHours(), d.toMinutes() == 0 ? "s" : d.toHours() == 0 ? "m" : "h")).value()).orElse(null),
                                    false, 30));
                        }
                    });
                }
            }
        }
    }

}
