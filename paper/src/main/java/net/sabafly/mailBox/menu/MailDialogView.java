package net.sabafly.mailBox.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

@SuppressWarnings("UnstableApiUsage")
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
        body.add(DialogBody.plainMessage(miniMessage().deserialize(mail.content())));
        List<ActionButton> actions = new ArrayList<>();
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
                                                .build()
                                )
                )
        );
    }
}
