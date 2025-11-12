package net.sabafly.mailBox.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.utils.DateUtils;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.Bukkit;
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
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class MailTemplateEditMenu extends InventoryMenu<MailTemplateEditMenu> {

    private final MailTemplate template;

    public MailTemplateEditMenu(Player player, MailTemplate template) {
        super(player, 18, miniMessage().deserialize(config().messages.mailTemplateEditMenuTitle));
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
            meta.itemName(miniMessage().deserialize(config().messages.autoSend));
            meta.lore(List.of(miniMessage().deserialize(template.autoSend() ? config().messages.enabled : config().messages.disabled)));
        });
        clickRegistry.setItem(0, autoSend, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                template.setAutoSend(!template.autoSend());
                refresh();
            }
        });
        clickRegistry.setItem(1, getSender(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setSender), value -> {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(value);
                    if (!p.hasPlayedBefore()) return;
                    template.setSender(database().getUser(p.getUniqueId()));
                    refresh();
                }));
            } else if (clickType.isRightClick()) {
                template.setSender(null);
                refresh();
            }
        });
        clickRegistry.setItem(2, getStartTime(), (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setStartTime), value -> {
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
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setEndTime), value -> {
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
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setInterval), value -> {
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
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setPermissions), value -> {
                    template.setPermission(value);
                    refresh();
                }, template.permission(), false, 80));
            } else if (clickType.isRightClick()) {
                template.setPermission(null);
                refresh();
            }
        });
        ItemStack delete = new ItemStack(Material.BARRIER);
        delete.editMeta(meta -> meta.itemName(miniMessage().deserialize(config().messages.delete)));
        clickRegistry.setItem(8, delete, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                MailBox.getThreadedQueue().submit(() -> {
                    database().deleteMailTemplate(template);
                    ThreadUtils.runSync(player::closeInventory);
                });
            }
        });
        ItemStack paper = new ItemStack(Material.PAPER);
        paper.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.titleValue, TagResolver.builder().tag("title", Tag.inserting(plainText().deserialize(template.title()))).build()));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSet))
            ));
        });
        clickRegistry.setItem(9, paper, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setTitle), value -> {
                    template.title(value);
                    refresh();
                }, template.title(), false, 80));
            }
        });
        ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
        book.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.contentInfo, TagResolver.builder().tag("length", Tag.inserting(Component.text(template.content().length()))).build()));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionEdit))
            ));
        });
        clickRegistry.setItem(10, book, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new StringInputMenu(this, player, miniMessage().deserialize(config().messages.setContent), value -> {
                    template.content(value);
                    refresh();
                }, template.content(), true, 2000));
            }
        });
        ItemStack attachments = new ItemStack(Material.CHEST);
        attachments.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.attachments));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionEdit)),
                    miniMessage().deserialize("<yellow>" + template.attachment().size())
            ));
        });
        clickRegistry.setItem(11, attachments, (player, clickType) -> {
            if (clickType.isLeftClick()) {
                openMenu(new CreateMailMenu.AttachmentMenu(this, player, template.attachment(), this.template::attachment, true));
            }
        });
    }

    private @NotNull ItemStack getPermissions() {
        ItemStack permissions = new ItemStack(Material.NAME_TAG);
        permissions.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.permissions));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.permission()).orElse(config().messages.noValue))
            ));
        });
        return permissions;
    }

    private @NotNull ItemStack getInterval() {
        ItemStack interval = new ItemStack(Material.NAME_TAG);
        interval.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.interval));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.interval()).map(d -> d.toHours() + "h" + d.toMinutesPart() + "m" + d.toSecondsPart() + "s").orElse(config().messages.noValue))
            ));
        });
        return interval;
    }

    private @NotNull ItemStack getEndTime() {
        ItemStack endTime = new ItemStack(Material.NAME_TAG);
        endTime.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.endTime));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.endTime()).map(LocalDateTime::toString).orElse(config().messages.noValue))
            ));
        });
        return endTime;
    }

    private @NotNull ItemStack getStartTime() {
        ItemStack startTime = new ItemStack(Material.NAME_TAG);
        startTime.editMeta(meta -> {
            meta.itemName(miniMessage().deserialize(config().messages.startTime));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionUnset)),
                    miniMessage().deserialize("<yellow>" + Optional.ofNullable(template.startTime()).map(LocalDateTime::toString).orElse(config().messages.noValue))
            ));
        });
        return startTime;
    }

    private @NotNull ItemStack getSender() {
        ItemStack sender = new ItemStack(Material.PLAYER_HEAD);
        if (template.sender() != null) {
            sender.editMeta(meta -> {
                if (meta instanceof SkullMeta skullMeta) {
                    try {
                        skullMeta.setPlayerProfile((Bukkit.getOfflinePlayer(template.sender().uuid())).getPlayerProfile());
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            });
        }
        sender.editMeta(meta -> {
            meta.customName(miniMessage().deserialize(config().messages.senderValue, TagResolver.builder().tag("sender", Tag.inserting(miniMessage().deserialize(template.sender() == null ? config().messages.systemName : Optional.ofNullable(Bukkit.getOfflinePlayer(template.sender().uuid()).getName()).orElse(template.sender().uuid().toString())))).build()));
            meta.lore(List.of(
                    miniMessage().deserialize(config().messages.leftClickTo.replace("{action}", config().messages.clickActionSet)),
                    miniMessage().deserialize(config().messages.rightClickTo.replace("{action}", config().messages.clickActionUnset))
            ));
        });
        return sender;
    }
}
