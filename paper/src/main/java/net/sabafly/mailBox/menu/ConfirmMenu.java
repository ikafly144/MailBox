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
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class ConfirmMenu extends DialogMenu implements Menu {

    private final Menu parent;
    private final Consumer<Boolean> consumer;
    private final Component title;
    private final Component content;

    protected ConfirmMenu(@NotNull Player player, @NotNull Menu parent, @NotNull Component title, @NotNull Component content, @NotNull Consumer<Boolean> consumer) {
        super(player);
        this.parent = parent;
        this.consumer = consumer;
        this.title = title;
        this.content = content;
    }

    @Override
    public void callClose(@NotNull Player player) {
        parent.callClose(player);
    }

    @Override
    public void open() {
        parent.callClose(viewer);
        this.viewer.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(
                                DialogBody.plainMessage(content)
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("OK"))
                                .action(DialogAction.customClick((_, _) -> {
                                    consumer.accept(true);
                                    parent.open();
                                }, ClickCallback.Options.builder().build()))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .action(DialogAction.customClick((_, _) -> {
                                    consumer.accept(false);
                                    parent.open();
                                }, ClickCallback.Options.builder().build()))
                                .build()
                ))
        ));
    }
}
