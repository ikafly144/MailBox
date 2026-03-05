package net.sabafly.mailBox.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.brigadier.TagParseCommandSyntaxException;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.commands.arguments.MailTemplateArgumentType;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.MailUser;
import net.sabafly.mailBox.menu.CreateMailMenu;
import net.sabafly.mailBox.menu.InboxMenu;
import net.sabafly.mailBox.menu.MailTemplateMenu;
import net.sabafly.mailBox.menu.SendMailMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.database;

public class MailCommands implements LifecycleEventHandler<@NotNull ReloadableRegistrarEvent<@NotNull Commands>> {

    @NotNull
    private final MailBox plugin;

    public MailCommands(@NotNull MailBox plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        // Register commands here
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, this);
    }

    @Override
    public void run(ReloadableRegistrarEvent<@NotNull Commands> event) {
        event.registrar().register(
                Commands.literal("mail")
                        .then(Commands.literal("reload")
                                .requires(context -> context.getSender().hasPermission("mailbox.admin"))
                                .executes(context -> {
                                    MailBox.reload();
                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(config().messages.reloadSuccess));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("template")
                                .requires(context -> context.getSender().hasPermission("mailbox.template"))
                                .executes(context -> {
                                    if (!(context.getSource().getExecutor() instanceof Player player))
                                        throw new TagParseCommandSyntaxException("Player required");
                                    new MailTemplateMenu(player, 1).open();
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("send")
                                        .requires(context -> context.getSender().hasPermission("mailbox.template.send"))
                                        .then(Commands.argument("target", ArgumentTypes.players())
                                                .then(Commands.argument("template", new MailTemplateArgumentType())
                                                        .executes(context -> {
                                                            final var targets = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource());
                                                            final var template = context.getArgument("template", MailTemplate.class);
                                                            targets.stream()
                                                                    .map(target-> database().getUser(target.getUniqueId()))
                                                                    .forEach(user -> {
                                                                        final var mail = template.createMail(user);
                                                                        database().createMail(mail);
                                                                    });
                                                            context.getSource().getSender().sendMessage(miniMessage()
                                                                    .deserialize(
                                                                            config().messages.sendTemplateSuccess,
                                                                            Placeholder.component("count", Component.text(targets.size())),
                                                                            Placeholder.component("template", miniMessage().deserialize(template.title()))
                                                                    )
                                                            );
                                                            return targets.size();
                                                        })
                                                )
                                        )
                                )
                        )
                        .requires(source -> source.getExecutor() instanceof Player)
                        .requires(source -> source.getSender().hasPermission("mailbox.use"))
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player))
                                throw new TagParseCommandSyntaxException("Failed to parse tag");
                            new InboxMenu(player).open();
                            return Command.SINGLE_SUCCESS;
                        })
                        .build(), List.of("mailbox")
        );
        event.registrar().register(Commands.literal("sendmail")
                .requires(context -> context.getSender().hasPermission("mailbox.send"))
                .executes(context -> {
                    if (!(context.getSource().getExecutor() instanceof Player player))
                        throw new TagParseCommandSyntaxException("Player required");
                    new SendMailMenu(player).open();
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("to", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            if (!(context.getSource().getExecutor() instanceof Player))
                                return builder.buildFuture();
                            var databaseIds = database().getAllUsers().stream()
                                    .map(MailUser::uuid);
                            var onlinePlayerIds = Bukkit.getOnlinePlayers().stream()
                                    .map(Player::getUniqueId)
                                    .collect(Collectors.toSet());
                            var allIds = databaseIds.collect(Collectors.toCollection(() -> onlinePlayerIds));
                            for (var id : allIds) {
                                var offlinePlayer = Bukkit.getOfflinePlayer(id);
                                builder.suggest(offlinePlayer.getName());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player))
                                throw new TagParseCommandSyntaxException("Player required");
                            OfflinePlayer target = Bukkit.getOfflinePlayer(StringArgumentType.getString(context, "to"));
                            if (!database().isUserExists(target.getUniqueId()))
                                throw new TagParseCommandSyntaxException("Player not found or has never played before");
                            if (player.getUniqueId().equals(target.getUniqueId()) && !player.hasPermission("mailbox.admin"))
                                throw new TagParseCommandSyntaxException("You can't send mail to yourself");
                            new CreateMailMenu(player, target).open();
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("from", ArgumentTypes.player())
                                .requires(commandSourceStack -> commandSourceStack.getSender() instanceof ConsoleCommandSender)
                                .executes(context -> {
                                    var to = Bukkit.getOfflinePlayer(StringArgumentType.getString(context, "to"));
                                    if (!database().isUserExists(to.getUniqueId()))
                                        throw new TagParseCommandSyntaxException("Player not found or has never played before");
                                    var from = context.getArgument("from", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
                                    if (from.getUniqueId().equals(to.getUniqueId()))
                                        throw new TagParseCommandSyntaxException("You can't send mail to the same player");
                                    new CreateMailMenu(from, to).open();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .build());
    }
}
