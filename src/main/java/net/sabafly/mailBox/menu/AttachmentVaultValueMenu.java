package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.mail.attachments.VaultValueAttachment;
import net.sabafly.mailBox.utils.EconomyUtils;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class AttachmentVaultValueMenu extends AnvilSetterMenu {

    public AttachmentVaultValueMenu(BaseMenu<?> parent, Player player, Consumer<VaultValueAttachment> consumer) {
        super(parent, player, miniMessage().deserialize(config().messages.attachmentAppendVault
                .replace("{currency}", EconomyUtils.getEconomy().currencyNamePlural())
        ), value -> {
            try {
                VaultValueAttachment.createNew(Double.parseDouble(value), player, config().mail.expirationTime.value()
                                .map(d -> Duration.ofSeconds(d.seconds()))
                                .orElse(null))
                        .ifPresent(consumer);
            } catch (Exception e) {
                MailBox.logger().error("Error while setting vault value attachment", e);
            }
        });
    }
}
