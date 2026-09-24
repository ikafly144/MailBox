package net.sabafly.mailBox.menu;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.PlayerMailUser;
import net.sabafly.mailBox.utils.DateUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.messages;
import static net.sabafly.mailBox.MailBox.database;

public class MailTemplateEditMenu extends InventoryMenu<MailTemplateEditMenu> {

    private final MailTemplate template;

    public MailTemplateEditMenu(Player player, MailTemplate template) {
        super(player, 18, miniMessage().deserialize(messages().mailTemplateEditMenuTitle));
        this.template = template;
    }

    @Override
    protected void onClose(@NotNull Player player, @Nullable InventoryView inventory) {
        database().updateMailTemplate(new MailTemplate(template));
        setNextMenu(new MailTemplateMenu(player, 1));
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
        ItemStack autoSend = new ItemStack(template.autoSend() ? Material.LIME_DYE : Material.GRAY_DYE);
        autoSend.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().autoSend));
            meta.lore(List.of(miniMessage().deserialize(template.autoSend() ? messages().enabled : messages().disabled)));
        });
        clickRegistry.setItem(0, autoSend, (_, clickType) -> {
            if (clickType.isLeftClick()) {
                template.setAutoSend(!template.autoSend());
                refresh();
            }
        });
        clickRegistry.setItem(1, getSender(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setSender), value -> {
                    @SuppressWarnings("PatternValidation")
                    var sender = database().getUserByAddress(Key.key(value));
                    if (sender == null) {
                        throw new IllegalStateException("User not found!");
                    }
                    template.setSender(sender);
                    refresh();
                }));
            } else if (clickType.isRightClick()) {
                template.setSender(MailBox.getInstance().getSystemUser());
                refresh();
            }
        });
        clickRegistry.setItem(2, getStartTime(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setStartTime), value -> {
                    template.setStartTime(DateUtils.parse(value));
                    refresh();
                }, Optional.ofNullable(template.startTime()).map(DateUtils::format).orElse(DateUtils.format(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).withSecond(0).withNano(0))),
                        false, 30));
            } else if (clickType.isRightClick()) {
                template.setStartTime(null);
                refresh();
            }
        });
        clickRegistry.setItem(3, getEndTime(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setEndTime), value -> {
                    template.setEndTime(DateUtils.parse(value));
                    refresh();
                }, Optional.ofNullable(template.endTime()).map(DateUtils::format).orElse(DateUtils.format(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).withSecond(0).withNano(0))),
                        false, 30));
            } else if (clickType.isRightClick()) {
                template.setEndTime(null);
                refresh();
            }
        });
        clickRegistry.setItem(4, getInterval(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setInterval), value -> {
                    template.setInterval(Duration.ofSeconds(io.papermc.paper.configuration.type.Duration.of(value).seconds()));
                    refresh();
                }, Optional.ofNullable(template.interval()).map(d -> io.papermc.paper.configuration.type.Duration.of("%d%s".formatted(d.toMinutes() == 0 ? d.toSeconds() : d.toHours() == 0 ? d.toMinutes() : d.toHours(), d.toMinutes() == 0 ? "s" : d.toHours() == 0 ? "m" : "h")).value()).orElse(null),
                        false, 12));
            } else if (clickType.isRightClick()) {
                template.setInterval(null);
                refresh();
            }
        });
        clickRegistry.setItem(5, getPermissions(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setPermissions), value -> {
                    template.setPermission(value);
                    refresh();
                }, template.permission(), false, 80));
            } else if (clickType.isRightClick()) {
                template.setPermission(null);
                refresh();
            }
        });
        ItemStack delete = new ItemStack(Material.BARRIER);
        delete.editMeta(meta -> meta.itemName(miniMessage().deserialize(messages().delete)));
        clickRegistry.setItem(8, delete, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                if (clickType.isShiftClick()) {
                    database().deleteMailTemplate(template);
                    player.closeInventory();
                    return;
                }
                new ConfirmMenu(
                        viewer,
                        this,
                        miniMessage().deserialize(messages().deleteMailConfirmTitle),
                        miniMessage().deserialize(
                                messages().deleteMailConfirmContent,
                                Placeholder.component("mail_title", plainText().deserialize(template.subject()))
                        ),
                        ok -> {
                            if (ok) database().deleteMailTemplate(template);
                            player.closeInventory();
                        }).open();
            }
        });
        ItemStack paper = new ItemStack(Material.PAPER);
        paper.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().subjectValue, TagResolver.builder().tag("subject", Tag.inserting(plainText().deserialize(template.subject()))).build()));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionSet))
            ));
        });
        clickRegistry.setItem(9, paper, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setSubject), value -> {
                    template.setSubject(value);
                    refresh();
                }, template.subject(), false, 80));
            }
        });
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        book.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().contentInfo, TagResolver.builder().tag("length", Tag.inserting(Component.text(template.content().length()))).build()));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionEdit))
            ));
        });
        clickRegistry.setItem(10, book, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(messages().setContent), value -> {
                    template.setContent(value);
                    refresh();
                }, template.content(), true, 2000));
            }
        });
        ItemStack attachments = new ItemStack(Material.CHEST);
        attachments.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().attachments));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionEdit)),
                    miniMessage().deserialize("<yellow>" + template.attachment().size())
            ));
        });
        clickRegistry.setItem(11, attachments, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new CreateMailMenu.AttachmentMenu(this, player, template.attachment(), this.template::setAttachment, true));
            }
        });
    }

    private @NotNull ItemStack getPermissions() {
        ItemStack permissions = new ItemStack(Material.NAME_TAG);
        permissions.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().permissions));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionSet)),
                    miniMessage().deserialize(messages().rightClickTo.replace("{action}", messages().clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.permission()).orElse(messages().noValue))
            ));
        });
        return permissions;
    }

    private @NotNull ItemStack getInterval() {
        ItemStack interval = new ItemStack(Material.NAME_TAG);
        interval.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().interval));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionSet)),
                    miniMessage().deserialize(messages().rightClickTo.replace("{action}", messages().clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.interval()).map(d -> d.toHours() + "h" + d.toMinutesPart() + "m" + d.toSecondsPart() + "s").orElse(messages().noValue))
            ));
        });
        return interval;
    }

    private @NotNull ItemStack getEndTime() {
        ItemStack endTime = new ItemStack(Material.NAME_TAG);
        endTime.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().endTime));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionSet)),
                    miniMessage().deserialize(messages().rightClickTo.replace("{action}", messages().clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.endTime()).map(LocalDateTime::toString).orElse(messages().noValue))
            ));
        });
        return endTime;
    }

    private @NotNull ItemStack getStartTime() {
        ItemStack startTime = new ItemStack(Material.NAME_TAG);
        startTime.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(messages().startTime));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionSet)),
                    miniMessage().deserialize(messages().rightClickTo.replace("{action}", messages().clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.startTime()).map(LocalDateTime::toString).orElse(messages().noValue))
            ));
        });
        return startTime;
    }

    private @NotNull ItemStack getSender() {
        ItemStack sender = new ItemStack(Material.PLAYER_HEAD);
        if (template.sender() instanceof PlayerMailUser(OfflinePlayer offlinePlayer, _)) {
            sender.editMeta(meta -> {
                if (meta instanceof SkullMeta skullMeta) {
                    try {
                        skullMeta.setPlayerProfile(offlinePlayer.getPlayerProfile());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });
        }
        sender.editMeta(meta -> {
            meta.customName(miniMessage().deserialize(messages().senderValue, TagResolver.builder().tag("sender", Tag.inserting(miniMessage().deserialize(template.sender().name()))).build()));
            meta.lore(List.of(
                    miniMessage().deserialize(messages().leftClickTo.replace("{action}", messages().clickActionSet)),
                    miniMessage().deserialize(messages().rightClickTo.replace("{action}", messages().clickActionUnset))
            ));
        });
        return sender;
    }
}
