package net.sabafly.mailBox.menu;

import net.sabafly.mailBox.mail.attachments.CommandAttachment;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class AttachmentCommandMenu extends AnvilSetterMenu {

    public AttachmentCommandMenu(CreateMailMenu.AttachmentMenu parent, Player player, Consumer<CommandAttachment> consumer) {
        super(parent, player, miniMessage().deserialize(config().messages.attachmentAppendCommand), s -> {
            List<String> args = Arrays.stream(s.split(";")).toList();
            if (args.size() == 2) {
                consumer.accept(new CommandAttachment(args.get(0), args.get(1), false, null, config().mail.expirationTime.value().map(d -> LocalDateTime.now().plus(Duration.ofSeconds(d.seconds()))).orElse(null)));
            }
        });
    }
}
