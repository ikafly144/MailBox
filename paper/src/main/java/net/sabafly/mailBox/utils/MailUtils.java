package net.sabafly.mailBox.utils;

import net.sabafly.mailBox.mail.Mail;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.meta.SkullMeta;

import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

@SuppressWarnings("UnstableApiUsage")
public class MailUtils {

    public static ItemStack mailHeadItem(Mail mail) {
        var senderItem = ItemType.PLAYER_HEAD.createItemStack();

        senderItem.editMeta(SkullMeta.class, meta -> {
            try {
                var profile = Bukkit.createProfile(mail.getSender().id(), mail.getSender().name());
                var texture = profile.getTextures();
                texture.setSkin(mail.getSender().skinUrl());
                profile.setTextures(texture);
                meta.setPlayerProfile(profile);
            } catch (IllegalArgumentException ignored) {
            }
        });
        senderItem.editMeta(meta -> meta.itemName(plainText().deserialize(mail.getSender().name())));
        return senderItem;
    }

}
