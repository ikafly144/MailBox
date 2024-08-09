package net.sabafly.mailbox.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.sabafly.mailbox.library.LibNMSAccessor;
import net.sabafly.mailbox.MailBox;
import net.sabafly.mailbox.commands.arguments.DateArgumentType;
import net.sabafly.mailbox.commands.arguments.MailActionArgumentType;
import net.sabafly.mailbox.configurations.Configurations;
import net.sabafly.mailbox.configurations.PluginConfiguration;
import net.sabafly.mailbox.type.MailAction;
import net.sabafly.mailbox.type.MailBoxEntry;
import net.sabafly.mailbox.utils.ItemUtil;
import net.sabafly.mailbox.utils.MailUtil;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurateException;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class SendMailCommand extends AbstractCommand {
    @Override
    public LiteralCommandNode<CommandSourceStack> command() {
        return Commands.literal("sendmail")
                .requires(source -> source.getSender().hasPermission("mailbox.command.sendmail"))
                .then(Commands.argument("display_item", ArgumentTypes.itemStack())
                        .then(Commands.argument("name", ArgumentTypes.component())
                                .then(Commands.argument("description", ArgumentTypes.component())
                                        .then(Commands.argument("action", MailActionArgumentType.mailAction())
                                                .then(Commands.argument("target", ArgumentTypes.players())
                                                        .executes(executor(context -> {
                                                            var targets = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
                                                            var item = context.getArgument("display_item", ItemStack.class);
                                                            var name = context.getArgument("name", Component.class);
                                                            var description = context.getArgument("description", Component.class);
                                                            var action = context.getArgument("action", MailAction.class);
                                                            targets.iterator().forEachRemaining(target -> {
                                                                var mail = MailBoxEntry.create(item, name, description, action);
                                                                MailUtil.sendMail(target, mail);
                                                            });
                                                            LibNMSAccessor.get().unwrapCommandSourceStack(context.getSource()).sendSuccess(() -> MiniMessage.miniMessage().deserialize(
                                                                    PluginConfiguration.get().messages.sentMails,
                                                                    TagResolver.builder()
                                                                            .tag("name", Tag.inserting(name))
                                                                            .tag("description", Tag.inserting(description))
                                                                            .tag("targets", Tag.inserting(Component.text(targets.size())))
                                                                            .tag("item", Tag.inserting(item.displayName()))
                                                                            .build()), true);
                                                            return targets.size();
                                                        }, false))
                                                )
                                                .then(Commands.literal("schedule")
                                                        .requires(source -> source.getSender().hasPermission("mailbox.command.sendmail.schedule"))
                                                        .then(Commands.argument("until", new DateArgumentType())
                                                                .executes(executor(context -> {
                                                                    var item = context.getArgument("display_item", ItemStack.class);
                                                                    var name = context.getArgument("name", Component.class);
                                                                    var description = context.getArgument("description", Component.class);
                                                                    var action = context.getArgument("action", MailAction.class);
                                                                    var until = context.getArgument("until", LocalDateTime.class);

                                                                    var config = PluginConfiguration.get();
                                                                    if (config.mailSchedules == null)
                                                                        config.mailSchedules = new ArrayList<>();
                                                                    config.mailSchedules.add(new PluginConfiguration.MailSchedule(JSONComponentSerializer.json().serialize(name), JSONComponentSerializer.json().serialize(description), until, ItemUtil.formatItem(item), action));
                                                                    try {
                                                                        Configurations.savePluginConfiguration(MailBox.getPlugin(MailBox.class).getDataPath().resolve("config.yml"));
                                                                    } catch (ConfigurateException e) {
                                                                        throw new RuntimeException(e);
                                                                    }

                                                                    LibNMSAccessor.get().unwrapCommandSourceStack(context.getSource()).sendSuccess(() -> MiniMessage.miniMessage().deserialize(
                                                                            PluginConfiguration.get().messages.scheduledMail,
                                                                            TagResolver.builder()
                                                                                    .tag("name", Tag.inserting(name))
                                                                                    .tag("description", Tag.inserting(description))
                                                                                    .tag("item", Tag.inserting(item.displayName()))
                                                                                    .tag("until", Tag.inserting(Component.text(until.toString())))
                                                                                    .build()), true);
                                                                    return Command.SINGLE_SUCCESS;
                                                                }, false))))
                                        )))
                )
                .build();
    }
}
