package net.sabafly.mailbox.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.sabafly.mailbox.library.LibNMSAccessor;
import net.sabafly.mailbox.MailBox;
import net.sabafly.mailbox.configurations.Configurations;
import net.sabafly.mailbox.configurations.PluginConfiguration;
import net.sabafly.mailbox.inventory.MailBoxInventory;
import net.sabafly.mailbox.type.MailBoxEntry;
import net.sabafly.mailbox.type.PDCListType;
import net.sabafly.mailbox.type.PDCMailBoxEntryType;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.ConfigurateException;

import java.util.ArrayList;

public class MailBoxCommand extends AbstractCommand {

    public MailBoxCommand() {
        super();
    }

    @Override
    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("mailbox")
                .requires(source -> source.getSender().hasPermission("mailbox.command.mailbox"))
                .executes(
                        executor((context) -> {
                            var player = (Player) context.getSource().getExecutor();
                            var container = player.getPersistentDataContainer();
                            var entry = container.getOrDefault(MailBoxEntry.KEY, new PDCListType<>(new PDCMailBoxEntryType()), new ArrayList<>());

                            player.openInventory(MailBoxInventory.create(entry, container).getInventory());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission("mailbox.command.reload"))
                        .executes(executor(context -> {
                            try {
                                Configurations.loadPluginConfiguration(MailBox.getPlugin(MailBox.class).getDataPath().resolve("config.yml"));
                            } catch (ConfigurateException e) {
                                throw new RuntimeException(e);
                            }
                            LibNMSAccessor.get().unwrapCommandSourceStack(context.getSource()).sendSuccess(() -> MiniMessage.miniMessage().deserialize(PluginConfiguration.get().messages.reload), true);
                            return Command.SINGLE_SUCCESS;
                        }, false))
                        .build()
                )
                .then(Commands.literal("clear")
                        .requires(source -> source.getSender().hasPermission("mailbox.command.clear"))
                        .executes(executor(context -> {
                            var player = (Player) context.getSource().getExecutor();
                            return clearMailBox(context.getSource(), player);
                        }))
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(executor(context -> {
                                    var target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
                                    return clearMailBox(context.getSource(), target);
                                }, false))
                                .build()
                        )
                )
                .build();
    }

    private int clearMailBox(CommandSourceStack source, Player target) {
        var container = target.getPersistentDataContainer();
        container.remove(MailBoxEntry.KEY);
        target.sendMessage(PluginConfiguration.get().messages.clearedMailBox);
        LibNMSAccessor.get().unwrapCommandSourceStack(source).sendSuccess(() -> MiniMessage.miniMessage().deserialize(PluginConfiguration.get().messages.clearMailBoxSuccess, TagResolver.builder().tag("target", Tag.inserting(target.name())).build()), true);
        return Command.SINGLE_SUCCESS;
    }

}
