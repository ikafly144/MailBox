package net.sabafly.mailBox.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

@SuppressWarnings("UnstableApiUsage")
public class SendMailMenu extends DialogMenu {

    @SuppressWarnings("PatternValidation")
    public static final DialogAction.CustomClickAction OPEN_SEND_MENU_ACTION = DialogAction.customClick((response, audience) -> {
        if (audience instanceof Player player) {
            String recipient = response.getText("recipient");
            if (recipient == null || recipient.isEmpty()) {
                player.sendMessage(miniMessage().deserialize(config().messages.emptyInputError));
                return;
            }
            if (!Key.parseable(recipient)) {
                player.sendMessage(miniMessage().deserialize(config().messages.invalidRecipientError));
                return;
            }
            try {
                User target = database().getUserByAddress(Key.key(recipient));
                if (target == null) {
                    player.sendMessage(miniMessage().deserialize(config().messages.notRegisteredError));
                    return;
                }
                new CreateMailMenu(player, target).open();
            } catch (Exception e) {
                player.sendMessage(miniMessage().deserialize(config().messages.invalidRecipientError));
            }
        }
    }, ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .build());

    private static void createDialog(@NotNull DialogRegistryEntry.Builder builder) {
        builder.type(DialogType.multiAction(List.of(
                                ActionButton.builder(miniMessage().deserialize(config().messages.nextButton))
                                        .action(OPEN_SEND_MENU_ACTION)
                                        .build()
                        ))
                        .exitAction(ActionButton.builder(Component.translatable("gui.cancel")).build())
                        .build())
                .base(DialogBase.builder(miniMessage().deserialize(config().messages.sendMailMenuTitle))
                        .inputs(List.of(
                                DialogInput.text("recipient", miniMessage().deserialize(config().messages.sendMailRecipientInput))
                                        .initial("")
                                        .maxLength(16)
                                        .build()
                        ))
                        .build());
    }

    public SendMailMenu(@NotNull Player player) {
        super(player);
    }

    @Override
    public void open() {
        viewer.showDialog(Dialog.create(factory -> createDialog(factory.empty())));
    }
}
