package net.sabafly.mailbox.utils;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.sabafly.mailbox.configurations.PluginConfiguration;
import net.sabafly.mailbox.type.MailBoxEntry;
import net.sabafly.mailbox.type.PDCListType;
import net.sabafly.mailbox.type.PDCMailBoxEntryType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MailUtil {

    private MailUtil() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static void sendMail(Player player, MailBoxEntry mail) {
        var container = player.getPersistentDataContainer();
        var oldEntry = container.getOrDefault(MailBoxEntry.KEY, new PDCListType<>(new PDCMailBoxEntryType()), List.of());
        var entry = new ArrayList<>(oldEntry);
        if (entry.stream().noneMatch(MailBoxEntry::isValid))
            entry.clear();
        entry.add(mail);
        container.remove(MailBoxEntry.KEY);
        container.set(MailBoxEntry.KEY, new PDCListType<>(new PDCMailBoxEntryType()), entry);
        player.playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP.getKey(), Sound.Source.PLAYER, 1, 2));
        player.sendMessage(MiniMessage.miniMessage().deserialize(PluginConfiguration.get().messages.receivedMail));
        if (player.hasPermission("mailbox.command.mailbox"))
            player.sendMessage(MiniMessage.miniMessage().deserialize(PluginConfiguration.get().messages.openMailBox));
    }

}
