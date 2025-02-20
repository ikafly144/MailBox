package net.sabafly.mailBox.listener;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailUser;
import net.sabafly.mailBox.schedule.ScheduleManager;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.SortedSet;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class PlayerListener implements Listener {

    public PlayerListener() {
    }

    public void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent event) {
        MailBox.getThreadedQueue().submit(() -> {
            MailUser user = database().getUser(event.getPlayer().getUniqueId());
            SortedSet<Mail> mails = database().getAllMails(user, TriState.FALSE);
            long unreceivedAttachments = database().getAllMails(user, TriState.NOT_SET)
                    .stream().mapToLong(mail -> mail.getAttachments().stream().filter(attachment -> !(attachment.isExpired() || attachment.received())).count()).sum();
            ThreadUtils.runSync(() -> {
                if (!mails.isEmpty()) {
                    event.getPlayer().sendMessage(miniMessage().deserialize(config().messages.unreadMail, TagResolver.builder().tag("count", Tag.inserting(Component.text(mails.size()))).build()));
                    event.getPlayer().playSound(Sound.sound().type(org.bukkit.Sound.UI_BUTTON_CLICK).pitch(2).build());
                }
                if (unreceivedAttachments > 0) {
                    event.getPlayer().sendMessage(miniMessage().deserialize(config().messages.unreceivedAttachment, TagResolver.builder().tag("count", Tag.inserting(Component.text(unreceivedAttachments))).build()));
                    event.getPlayer().playSound(Sound.sound().type(org.bukkit.Sound.UI_BUTTON_CLICK).pitch(2).build());
                }
            });
            ScheduleManager.checkNotify(event.getPlayer(), user);
        });
    }


}
