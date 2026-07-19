package net.sabafly.mailBox.schedule;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.PlayerMailUser;
import net.sabafly.mailBox.utils.PlaceholderUtils;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class ScheduleManager {

    private final MailBox plugin;
    @Nullable
    private ScheduledTask task;

    public ScheduleManager(MailBox plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, _ -> {
            List<MailTemplate> templates = database().getAllMailTemplates().stream()
                    .filter(MailTemplate::autoSend)
                    .filter(mailTemplate -> mailTemplate.startTime() != null && mailTemplate.startTime().isBefore(LocalDateTime.now()))
                    .filter(mailTemplate -> mailTemplate.endTime() == null || mailTemplate.endTime().isAfter(LocalDateTime.now()))
                    .toList();
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (!(database().getUser(player.getUniqueId()) instanceof PlayerMailUser playerMailUser)) {
                    return;
                }
                ThreadUtils.runSync(player, () -> {
                    for (MailTemplate template : templates) {
                        if (!Optional.ofNullable(template.permission()).map(permission -> player.permissionValue(permission) == TriState.TRUE).orElse(true)) {
                            continue;
                        }
                        long intervalCount = template.intervalCount();
                        Bukkit.getAsyncScheduler().runNow(plugin, _ -> {
                            if (database().hasUserTemplate(playerMailUser, template, (int) intervalCount)) {
                                Optional<LocalDateTime> time = database().getUserTemplateTime(playerMailUser, template, (int) intervalCount);
                                if (template.interval() == null || time.map(t -> Optional.ofNullable(template.interval()).map(Duration.between(t, LocalDateTime.now())::compareTo).map(i -> i<0).orElse(false)).orElse(false)) {
                                    return;
                                }
                                database().deleteUserTemplate(playerMailUser, template, (int) intervalCount);
                            }
                            Mail mail = template.createMail(playerMailUser);
                            database().createMail(mail);
                            database().createUserTemplate(playerMailUser, template, (int) intervalCount);
                        });
                    }
                    Bukkit.getAsyncScheduler().runNow(plugin, _ -> checkNotify(player, playerMailUser, false));
                });
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void checkNotify(Player player, PlayerMailUser user, boolean login) {
        if (!config().enableMailNotification) return;
        boolean notified = false;
        try {
            if (login) {
                SortedSet<Mail> mails = database().getAllMails(user, TriState.FALSE);
                long unreceivedAttachments = database().getAllMails(user, TriState.NOT_SET)
                        .stream().mapToLong(mail -> mail.getAttachmentsInternal().stream().filter(attachment -> !(attachment.isExpired() || attachment.opened())).count()).sum();
                if (!mails.isEmpty()) {
                    ThreadUtils.runSync(player, () -> {
                        player.sendMessage(PlaceholderUtils.deserialize(player, config().messages.unreadMail, TagResolver.builder().tag("count", Tag.inserting(Component.text(mails.size()))).build()));
                        player.playSound(Sound.sound().type(org.bukkit.Sound.UI_BUTTON_CLICK).pitch(2).build());
                    });
                    notified = true;
                }
                if (unreceivedAttachments > 0) {
                    ThreadUtils.runSync(player, () -> {
                        player.sendMessage(PlaceholderUtils.deserialize(player, config().messages.unreceivedAttachment, TagResolver.builder().tag("count", Tag.inserting(Component.text(unreceivedAttachments))).build()));
                        player.playSound(Sound.sound().type(org.bukkit.Sound.UI_BUTTON_CLICK).pitch(2).build());
                    });
                    notified = true;
                }
            }
            List<Mail> notifications = database().getAllUserNotification(user);
            database().deleteAllUserNotification(user);
            if (notifications.isEmpty()) return;
            int size = notifications.size();
            size -= (int) database().getAllMails(user, TriState.FALSE).stream()
                    .filter(Mail::isRead)
                    .count();
            if (size != 0) {
                final int finalSize = size;
                ThreadUtils.runSync(player, () -> {
                    player.sendMessage(PlaceholderUtils.deserialize(player, config().messages.newMail, TagResolver.builder().tag("count", Tag.inserting(Component.text(finalSize))).build()));
                    player.playSound(Sound.sound().type(org.bukkit.Sound.UI_TOAST_IN).build());
                });
                notified = true;
            }
        } finally {
            if (notified) {
                ThreadUtils.runSync(player, () -> player.sendMessage(PlaceholderUtils.deserialize(player, config().messages.howToOpenMail, TagResolver.builder().tag("command", Tag.inserting(Component.text("/mail"))).build())));
            }
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

}
