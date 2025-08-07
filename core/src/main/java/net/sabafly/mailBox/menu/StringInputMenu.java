package net.sabafly.mailBox.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

@SuppressWarnings("UnstableApiUsage")
public class StringInputMenu extends DialogMenu implements Menu {

    @NotNull
    private final Menu parent;
    @NotNull
    private final Consumer<String> consumer;
    @NotNull
    private final Component title;
    @NotNull
    private final String defaultText;
    private final boolean multiline;
    private final int maxLength;

    public StringInputMenu(@NotNull InventoryMenu<?> parent, Player player, @NotNull Component title, @NotNull final Consumer<String> consumer, @Nullable String defaultText, boolean multiline, int maxLength) {
        super(player);
        this.parent = parent;
        this.consumer = consumer;
        this.title = title;
        this.defaultText = defaultText == null ? "" : defaultText;
        this.multiline = multiline;
        this.maxLength = maxLength;
    }

    public StringInputMenu(@NotNull InventoryMenu<?> parent, Player player, @NotNull Component title, @NotNull final Consumer<String> consumer) {
        this(parent, player, title, consumer, "", false, 100);
    }

    @Override
    public void open() {
        parent.callClose(player);
        this.player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .inputs(List.of(
                                DialogInput.text("text", title)
                                        .initial(defaultText)
                                        .multiline(TextDialogInput.MultilineOptions.create(
                                                null,
                                                multiline ? 72 : 20
                                        ))
                                        .maxLength(maxLength)
                                        .labelVisible(false)
                                        .build()
                        ))
                        .build())
                .type(DialogType.multiAction(
                        List.of(ActionButton.builder(miniMessage().deserialize(config().messages.submitButton))
                                .action(DialogAction.customClick(
                                        (response, audience) -> {
                                            String text = response.getText("text");
                                            if (text == null) {
                                                text = "";
                                            }
                                            if (!multiline) text = text.replaceAll("\n", "");
                                            text = text.trim();
                                            if (!text.isBlank()) {
                                                try {
                                                    consumer.accept(text);
                                                } catch (Exception e) {
                                                    audience.sendMessage(Component.text("Error: " + e.getMessage()));
                                                }
                                            } else {
                                                audience.sendMessage(miniMessage().deserialize(config().messages.emptyInputError));
                                            }
                                            if ((audience instanceof Player)) {
                                                parent.open();
                                            }
                                        },
                                        ClickCallback.Options.builder().build()))
                                .build()),
                        ActionButton.builder(miniMessage().deserialize(config().messages.cancelButton))
                                .action(DialogAction.customClick((response, audience) -> {
                                    if ((audience instanceof Player)) {
                                        parent.open();
                                    }
                                }, ClickCallback.Options.builder().build()))
                                .build(),
                        1
                ))));

    }
}
