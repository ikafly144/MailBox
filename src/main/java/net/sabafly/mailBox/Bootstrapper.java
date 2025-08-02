package net.sabafly.mailBox;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.sabafly.mailBox.configuration.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public final class Bootstrapper implements PluginBootstrap {

    public static final @NotNull Key CONTENT_DIALOG_KEY = Key.key("mailbox:mail_menu");

    private ConfigLoader config;

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        this.config = new ConfigLoader(context.getDataDirectory());
        config.reload();
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose().newHandler(event -> event.registry().register(
                DialogKeys.create(CONTENT_DIALOG_KEY),
                builder -> builder.type(DialogType.multiAction(List.of(
                                        ActionButton.builder(miniMessage().deserialize(config.config().messages.inboxButton))
                                                .action(DialogAction.staticAction(ClickEvent.runCommand("/mailbox:mail")))
                                                .tooltip(miniMessage().deserialize(config.config().messages.inboxTooltip))
                                                .build(),
                                        ActionButton.builder(miniMessage().deserialize(config.config().messages.sendMailButton))
                                                .action(DialogAction.staticAction(ClickEvent.runCommand("/mailbox:sendmail")))
                                                .tooltip(miniMessage().deserialize(config.config().messages.sendMailTooltip))
                                                .build()
                                ))
                                .exitAction(ActionButton.builder(Component.translatable("gui.back")).build())
                                .columns(1)
                                .build())
                        .base(DialogBase.builder(miniMessage().deserialize(config.config().messages.mailMenuTitle)).build())
        )));
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG)
                .newHandler(event ->
                        event.registrar().addToTag(DialogTagKeys.QUICK_ACTIONS, List.of(
                                DialogKeys.create(CONTENT_DIALOG_KEY)
                        ))));
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG)
                .newHandler(event ->
                        event.registrar().addToTag(DialogTagKeys.PAUSE_SCREEN_ADDITIONS, List.of(
                                DialogKeys.create(CONTENT_DIALOG_KEY)
                        )))
        );
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new MailBox(config);
    }
}
