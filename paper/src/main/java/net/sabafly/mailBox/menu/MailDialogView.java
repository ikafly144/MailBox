package net.sabafly.mailBox.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.utils.MailUtils;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class MailDialogView extends DialogMenu {

    private final Menu parent;
    private final Mail mail;
    private final User owner;

    protected MailDialogView(@NotNull Player player, Menu parent, Mail mail, User owner) {
        super(player);
        this.parent = parent;
        this.mail = mail;
        this.owner = owner;
    }

    @Override
    public void open() {
        parent.callClose(viewer);
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(MailUtils.mailHeadItem(mail))
                .description(DialogBody.plainMessage(Component.text("THIS IS HEADER"))) //TODO
                .build()
        );
        body.add(DialogBody.plainMessage(miniMessage().deserialize(mail.content())));
        List<ActionButton> actions = new ArrayList<>();
        actions.add(ActionButton.builder(Component.text("Close"))
                .action(DialogAction.customClick((_, _) -> parent.open(), ClickCallback.Options.builder().build()))
                .build()
        );
        viewer.showDialog(
                Dialog.create(builder ->
                        builder.empty()
                                .base(
                                        DialogBase.builder(plainText().deserialize(mail.getTitle()))
                                                .body(body)
                                                .build()
                                )
                                .type(
                                        DialogType.multiAction(actions)
                                                .exitAction(ActionButton.builder(Component.translatable("gui.cancel"))
                                                        .action(DialogAction.customClick((_, _) -> parent.open(), ClickCallback.Options.builder().build()))
                                                        .build())
                                                .build()
                                )
                )
        );
    }
}
