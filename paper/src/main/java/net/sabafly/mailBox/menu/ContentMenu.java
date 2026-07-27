package net.sabafly.mailBox.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.messages;

@SuppressWarnings("UnstableApiUsage")
public class ContentMenu extends DialogMenu {

    @Nullable
    private final Menu parent;
    @NotNull
    private final Component title;
    @NotNull
    private final Component content;

    public static ContentMenu contentMenu(@NotNull Player player, @NotNull Component title, @NotNull Component content, @Nullable Menu parent) {
        return new ContentMenu(player, title, content, parent);
    }

    ContentMenu(Player player, @NotNull Component title, @NotNull Component content, @Nullable Menu parent) {
        super(player);
        this.parent = parent;
        this.title = title;
        this.content = content;
    }

    @Override
    public void open() {
        if (parent != null) parent.callClose(viewer);
        viewer.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                DialogBody.plainMessage(content)
                        ))
                        .build())
                .type(DialogType.notice(ActionButton.builder(miniMessage().deserialize(messages().closeButton))
                        .action(DialogAction.customClick((_, _) -> {
                            if (parent != null) parent.open();
                        }, ClickCallback.Options.builder().build()))
                        .build()))
        ));
    }
}
