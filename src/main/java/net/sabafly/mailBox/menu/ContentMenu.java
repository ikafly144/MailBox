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

import java.util.List;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;
import static net.sabafly.mailBox.MailBox.config;

@SuppressWarnings("UnstableApiUsage")
public class ContentMenu extends BaseMenu<ContentMenu> {

    @NotNull
    private final BaseMenu<?> parent;
    @NotNull
    private final String title;
    @NotNull
    private final String content;

    ContentMenu(Player player, @NotNull String title, @NotNull String content, @NotNull BaseMenu<?> parent) {
        super(player, 9, Component.text("content_menu"));
        this.parent = parent;
        this.title = title;
        this.content = content.replaceAll("§", "");
    }

    @Override
    public void open() {
        parent.callClose(player, player.getOpenInventory());
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(miniMessage().deserialize(title))
                        .body(List.of(
                                DialogBody.plainMessage(plainText().deserialize(content))
                        ))
                        .build())
                .type(DialogType.notice(ActionButton.builder(miniMessage().deserialize(config().messages.closeButton))
                                .action(DialogAction.customClick((response, audience) -> parent.open(), ClickCallback.Options.builder().build()))
                        .build()))
        ));
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
    }
}
