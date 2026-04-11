package net.sabafly.mailBox.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.brigadier.TagParseCommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.commands.arguments.DurationArgumentType;
import net.sabafly.mailBox.commands.arguments.MailTemplateArgumentType;
import net.sabafly.mailBox.mail.IAttachment;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.PlayerMailUser;
import net.sabafly.mailBox.mail.attachments.CommandAttachment;
import net.sabafly.mailBox.mail.attachments.ItemAttachment;
import net.sabafly.mailBox.mail.attachments.MessageAttachment;
import net.sabafly.mailBox.mail.attachments.VaultValueAttachment;
import net.sabafly.mailBox.menu.CreateMailMenu;
import net.sabafly.mailBox.menu.InboxMenu;
import net.sabafly.mailBox.menu.MailTemplateMenu;
import net.sabafly.mailBox.menu.SendMailMenu;
import net.sabafly.mailbox.api.mail.User;
import net.sabafly.mailbox.api.mail.attachments.Attachment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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

    private static final SuggestionProvider<CommandSourceStack> MAIL_USER_SUGGESTION = (context, builder) -> {
        if (!(context.getSource().getExecutor() instanceof Player))
            return builder.buildFuture();
        database().getAllUsers().stream()
                .map(User::key)
                .map(Key::asMinimalString)
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

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
                                                                    .map(Player::getUniqueId)
                                                                    .map(database()::<PlayerMailUser>getUser)
                                                                    .filter(Objects::nonNull)
                                                                    .forEach(template::send);
                                                            context.getSource().getSender().sendMessage(miniMessage()
                                                                    .deserialize(
                                                                            config().messages.sendTemplateSuccess,
                                                                            Placeholder.component("count", Component.text(targets.size())),
                                                                            Placeholder.component("template", miniMessage().deserialize(template.subject()))
                                                                    )
                                                            );
                                                            return targets.size();
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("create")
                                        .requires(context -> context.getSender().hasPermission("mailbox.template.create"))
                                        .then(Commands.argument("subject", StringArgumentType.string())
                                                .executes(context -> {
                                                    String subject = context.getArgument("subject", String.class);
                                                    if (database().getAllMailTemplates().stream().anyMatch(template -> template.subject().equals(subject))) {
                                                        context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                config().messages.templateAlreadyExists,
                                                                Placeholder.component("subject", miniMessage().deserialize(subject))
                                                        ));
                                                        return Command.SINGLE_SUCCESS;
                                                    }
                                                    var template = MailTemplate.TemplateBuilder.builder()
                                                            .subject(subject)
                                                            .build();
                                                    database().createMailTemplate(template);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("delete")
                                        .requires(context -> context.getSender().hasPermission("mailbox.template.delete"))
                                        .then(Commands.argument("template", new MailTemplateArgumentType())
                                                .executes(context -> {
                                                    var template = context.getArgument("template", MailTemplate.class);
                                                    database().deleteMailTemplate(template);
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                                .then(Commands.literal("edit")
                                        .requires(context -> context.getSender().hasPermission("mailbox.template.edit"))
                                        .then(Commands.argument("template", new MailTemplateArgumentType())
                                                .then(Commands.literal("subject")
                                                        .then(Commands.argument("new_subject", StringArgumentType.string())
                                                                .executes(context -> {
                                                                    var template = context.getArgument("template", MailTemplate.class);
                                                                    String newSubject = context.getArgument("new_subject", String.class);
                                                                    if (database().getAllMailTemplates().stream().anyMatch(t -> t.subject().equals(newSubject))) {
                                                                        context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                config().messages.templateAlreadyExists,
                                                                                Placeholder.component("subject", miniMessage().deserialize(newSubject))
                                                                        ));
                                                                        return Command.SINGLE_SUCCESS;
                                                                    }
                                                                    template.setSubject(newSubject);
                                                                    database().updateMailTemplate(template);
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("content")
                                                        .then(Commands.argument("new_content", ArgumentTypes.component())
                                                                .executes(context -> {
                                                                    var template = context.getArgument("template", MailTemplate.class);
                                                                    Component newContent = context.getArgument("new_content", Component.class);
                                                                    template.setContent(miniMessage().serialize(newContent));
                                                                    database().updateMailTemplate(template);
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("sender")
                                                        .then(Commands.argument("new_sender", ArgumentTypes.key())
                                                                .suggests(MAIL_USER_SUGGESTION)
                                                                .executes(context -> {
                                                                    var template = context.getArgument("template", MailTemplate.class);
                                                                    var newSenderKey = context.getArgument("new_sender", Key.class);
                                                                    var newSender = database().getUserByAddress(newSenderKey);
                                                                    if (newSender == null)
                                                                        throw new TagParseCommandSyntaxException("User not found or has never played before");
                                                                    template.setSender(newSender);
                                                                    database().updateMailTemplate(template);
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("attachment")
                                                        .then(Commands.literal("list")
                                                                .executes(context -> {
                                                                    var template = context.getArgument("template", MailTemplate.class);
                                                                    var attachments = database().getTemplateAttachments(template);
                                                                    if (attachments.isEmpty()) {
                                                                        context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                config().messages.templateNoAttachments,
                                                                                Placeholder.component("template", miniMessage().deserialize(template.subject()))
                                                                        ));
                                                                    } else {
                                                                        context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                config().messages.templateAttachmentList,
                                                                                Placeholder.component("template", miniMessage().deserialize(template.subject())),
                                                                                Placeholder.component("attachments", Component.join(
                                                                                        JoinConfiguration.separator(Component.newline()),
                                                                                        Streams.mapWithIndex(
                                                                                                        attachments.stream()
                                                                                                                .map(Attachment::format),
                                                                                                        (element, idx) ->
                                                                                                                Optional.of(element)
                                                                                                                        .map(e -> Component.text((idx + 1) + ": ").append(e))
                                                                                                                        .orElse(Component.text((idx + 1) + ": <null>:"))
                                                                                                )
                                                                                                .toList()
                                                                                ))
                                                                        ));
                                                                    }
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                        .then(
                                                                addAttachmentNode(Commands.literal("add"))
                                                        )
                                                        .then(Commands.literal("delete")
                                                                .then(Commands.argument("attachment_index", IntegerArgumentType.integer(1))
                                                                        .executes(context -> {
                                                                            var template = context.getArgument("template", MailTemplate.class);
                                                                            int index = context.getArgument("attachment_index", Integer.class) - 1;
                                                                            var attachments = database().getTemplateAttachments(template);
                                                                            if (index < 0 || index >= attachments.size()) {
                                                                                context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                        config().messages.invalidAttachmentIndex,
                                                                                        Placeholder.component("index", Component.text(index + 1))
                                                                                ));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            }
                                                                            var attachment = attachments.get(index);
                                                                            database().deleteTemplateAttachment(template, attachment);
                                                                            context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                    config().messages.templateAttachmentDeleteSuccess,
                                                                                    Placeholder.component("template", miniMessage().deserialize(template.subject())),
                                                                                    Placeholder.component("attachment", attachment.format())
                                                                            ));
                                                                            return Command.SINGLE_SUCCESS;
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("inbox")
                                .requires(source -> source.getExecutor() instanceof Player)
                                .requires(source -> source.getSender().hasPermission("mailbox.inbox"))
                                .executes(context -> {
                                    if (!(context.getSource().getExecutor() instanceof Player player))
                                        throw new TagParseCommandSyntaxException("Failed to parse tag");
                                    var user = database().getUser(player.getUniqueId());
                                    if (user == null)
                                        throw new IllegalStateException("User not found");
                                    new InboxMenu(player, user).open();
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("of")
                                        .requires(source -> source.getSender().hasPermission("mailbox.inbox.others"))
                                        .then(Commands.argument("source", ArgumentTypes.key())
                                                .suggests(MAIL_USER_SUGGESTION)
                                                .executes(context -> {
                                                    if (!(context.getSource().getExecutor() instanceof Player player))
                                                        throw new TagParseCommandSyntaxException("Player required");
                                                    var source = context.getArgument("source", Key.class);
                                                    var user = database().getUserByAddress(source);
                                                    if (user == null)
                                                        throw new TagParseCommandSyntaxException("User not found or has never played before");
                                                    new InboxMenu(player, user).open();
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
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
                .then(Commands.argument("to", ArgumentTypes.key())
                        .suggests(MAIL_USER_SUGGESTION)
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player))
                                throw new TagParseCommandSyntaxException("Player required");
                            var target = context.getArgument("to", Key.class);
                            var user = database().getUserByAddress(target);
                            if (user == null)
                                throw new TagParseCommandSyntaxException("User not found or has never played before");
                            if (player.getUniqueId().equals(user.id()) && !player.hasPermission("mailbox.admin"))
                                throw new TagParseCommandSyntaxException("You can't send mail to yourself");
                            new CreateMailMenu(player, user).open();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build());
    }

    private static <A extends ArgumentBuilder<CommandSourceStack, A>> A addAttachmentNode(A node) {
        return node.then(Commands.literal("type")
                .then(Commands.literal("item")
                        .then(Commands.argument("item", ArgumentTypes.itemStack())
                                .then(
                                        attachmentIcon(
                                                Commands.argument("amount", IntegerArgumentType.integer(1, 6400)),
                                                c -> {
                                                    var item = c.getArgument("item", ItemStack.class);
                                                    var amount = c.getArgument("amount", Integer.class);
                                                    item.setAmount(amount);
                                                    return ItemAttachment.ItemAttachmentBuilder.builder(item);
                                                }
                                        )
                                )
                        )
                )
                .then(Commands.literal("command")
                        .then(
                                attachmentName(
                                        Commands.argument("command", StringArgumentType.string()),
                                        c -> {
                                            var command = c.getArgument("command", String.class);
                                            return CommandAttachment.CommandAttachmentBuilder.builder(command);
                                        }
                                )
                        )
                )
                .then(Commands.literal("vault_value")
                        .requires(_ -> MailBox.isVaultEnabled())
                        .then(
                                attachmentIcon(
                                        Commands.argument("amount", FloatArgumentType.floatArg(0.001f)),
                                        c -> {
                                            var amount = c.getArgument("amount", Float.class);
                                            return VaultValueAttachment.VaultValueAttachmentBuilder.builder(amount);
                                        })
                        )
                )
                .then(Commands.literal("message")
                        .then(
                                attachmentName(
                                        Commands.argument("message", ArgumentTypes.component()),
                                        c -> {
                                            var message = c.getArgument("message", Component.class);
                                            return MessageAttachment.MessageAttachmentBuilder.builder(message);
                                        }
                                )
                        )
                )
        );
    }

    private static <A extends ArgumentBuilder<CommandSourceStack, A>> A attachmentName(
            A node, Function<CommandContext<CommandSourceStack>, Attachment.Builder<?, ?>> attachmentGetter
    ) {
        return node.then(Commands.literal("name")
                .then(
                        attachmentIcon(
                                Commands.argument("name", ArgumentTypes.component()),
                                context -> {
                                    var name = context.getArgument("name", Component.class);
                                    var builder = attachmentGetter.apply(context);
                                    builder.name(miniMessage().serialize(name));
                                    return builder;
                                }
                        )
                )
        );
    }

    private static <A extends ArgumentBuilder<CommandSourceStack, A>> A attachmentIcon(
            A node, Function<CommandContext<CommandSourceStack>, Attachment.Builder<?, ?>> attachmentGetter
    ) {
        return node.then(Commands.literal("icon")
                .then(
                        attachmentExpires(
                                Commands.argument("item", ArgumentTypes.itemStack()),
                                context -> {
                                    var item = context.getArgument("item", ItemStack.class);
                                    var builder = attachmentGetter.apply(context);
                                    builder.icon(item);
                                    return builder;
                                }
                        )
                )
                .executes(attachmentExpires(node, attachmentGetter).build().getCommand())
        );
    }

    private static <A extends ArgumentBuilder<CommandSourceStack, A>> A attachmentExpires(
            A node, Function<CommandContext<CommandSourceStack>, Attachment.Builder<?, ?>> attachmentGetter
    ) {
        return node.then(Commands.literal("expires")
                .then(
                        buildAttachment(
                                Commands.argument("duration", DurationArgumentType.duration()),
                                context -> {
                                    var duration = context.getArgument("duration", Duration.class);
                                    var builder = attachmentGetter.apply(context);
                                    builder.expireDuration(duration);
                                    return builder;
                                }
                        )
                )
                .executes(buildAttachment(node, attachmentGetter).build().getCommand())
        );
    }

    private static <A extends ArgumentBuilder<CommandSourceStack, A>> A buildAttachment(
            A node, Function<CommandContext<CommandSourceStack>, Attachment.Builder<?, ?>> attachmentGetter
    ) {
        return node.executes(context -> {
            var template = context.getArgument("template", MailTemplate.class);
            var attachment = attachmentGetter.apply(context).build();
            if (!(attachment instanceof IAttachment<?, ?> iAttachment))
                throw new IllegalStateException("Built attachment is not an instance of IAttachment");
            database().createTemplateAttachment(template, iAttachment);
            context.getSource().getSender().sendMessage(miniMessage().deserialize(
                    config().messages.templateAttachmentAddSuccess,
                    Placeholder.component("template", miniMessage().deserialize(template.subject())),
                    Placeholder.component("attachment", attachment.format())
            ));
            return Command.SINGLE_SUCCESS;
        });
    }

}
