package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.attachments.VaultValueAttachment;
import net.sabafly.mailBox.utils.EconomyUtils;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.regex.Matcher;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class AttachmentVaultValueMenu extends StringInputMenu {

    public AttachmentVaultValueMenu(InventoryMenu<?> parent, Player player, Consumer<VaultValueAttachment> consumer) {
        super(parent, player, miniMessage().deserialize(config().messages.attachmentAppendVault
               .replaceAll("\\{currency}", Matcher.quoteReplacement(EconomyUtils.getEconomy().currencyNamePlural()))
        ), value -> {
            try {
                consumer.accept(VaultValueAttachment.createNew(Double.parseDouble(value)));
            } catch (Exception e) {
                MailBox.logger().error("Error while setting vault value attachment", e);
            }
        });
    }
}
