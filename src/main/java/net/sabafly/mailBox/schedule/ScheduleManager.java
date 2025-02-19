package net.sabafly.mailBox.schedule;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.MailUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
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
        task = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            List<MailTemplate> templates = database().getAllMailTemplates().stream()
                    .filter(MailTemplate::autoSend)
                    .filter(mailTemplate -> mailTemplate.startTime() != null && mailTemplate.startTime().isBefore(LocalDateTime.now()))
                    .filter(mailTemplate -> mailTemplate.endTime() == null || mailTemplate.endTime().isAfter(LocalDateTime.now()))
                    .toList();
            Bukkit.getOnlinePlayers().forEach(player -> {
                final MailUser user = database().getUser(player.getUniqueId());
                for (MailTemplate template : templates) {
                    if (!Optional.ofNullable(template.permission()).map(player::hasPermission).orElse(true)) continue;
                    long intervalCount = Optional.ofNullable(template.interval()).map(d -> Duration.between(Objects.requireNonNull(template.startTime()), LocalDateTime.now()).dividedBy(d)).orElse(0L);
                    MailBox.getThreadedQueue().submit(() -> {
                        if (database().hasUserTemplate(user, template, (int) intervalCount)) return;
                        Mail mail = template.createMail(user);
                        database().createMail(mail);
                        database().createUserTemplate(user, template, (int) intervalCount);
                    });
                }
                MailBox.getThreadedQueue().submit(() -> checkNotify(player, user));
            });
        }, 0, 1, TimeUnit.MINUTES);
    }

    public static void checkNotify(Player player, MailUser user) {
        List<Mail> mails = database().getAllUserNotification(user);
        database().deleteAllUserNotification(user);
        if (mails.isEmpty()) return;
        player.sendMessage(miniMessage().deserialize(config().messages.newMail, TagResolver.builder().tag("count", Tag.inserting(Component.text(mails.size()))).build()));
        player.playSound(Sound.sound().type(org.bukkit.Sound.UI_TOAST_IN).build());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

}
