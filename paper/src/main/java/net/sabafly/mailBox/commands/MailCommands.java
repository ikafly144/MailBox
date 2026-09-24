package net.sabafly.mailBox.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailBox.commands.arguments.DurationArgumentType;
import net.sabafly.mailBox.commands.arguments.MailTemplateArgumentType;
import net.sabafly.mailBox.commands.arguments.MailUserArgumentType;
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
import static net.sabafly.mailBox.MailBox.messages;
import static net.sabafly.mailBox.MailBox.database;

public class MailCommands implements LifecycleEventHandler<@NotNull ReloadableRegistrarEvent<@NotNull Commands>> {

    private static final SimpleCommandExceptionType ERROR_PLAYER_REQUIRED = new SimpleCommandExceptionType(() -> "Player required");
    private static final SimpleCommandExceptionType ERROR_CANNOT_SEND_YOURSELF = new SimpleCommandExceptionType(() -> "You can't send mail to yourself");

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
                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(messages().reloadSuccess));
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("template")
                                .requires(context -> context.getSender().hasPermission("mailbox.template"))
                                .executes(context -> {
                                    if (!(context.getSource().getExecutor() instanceof Player player))
                                        throw ERROR_PLAYER_REQUIRED.create();
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
                                                                            messages().sendTemplateSuccess,
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
                                                                messages().templateAlreadyExists,
                                                                Placeholder.component("subject", miniMessage().deserialize(subject))
                                                        ));
                                                        return Command.SINGLE_SUCCESS;
                                                    }
                                                    var template = MailTemplate.TemplateBuilder.builder()
                                                            .subject(subject)
                                                            .build();
                                                    database().createMailTemplate(template);
                                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                            messages().templateCreateSuccess,
                                                            Placeholder.component("template", miniMessage().deserialize(template.subject()))
                                                    ));
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
                                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                            messages().templateDeleteSuccess,
                                                            Placeholder.component("template", miniMessage().deserialize(template.subject()))
                                                    ));
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
                                                                                messages().templateAlreadyExists,
                                                                                Placeholder.component("subject", miniMessage().deserialize(newSubject))
                                                                        ));
                                                                        return Command.SINGLE_SUCCESS;
                                                                    }
                                                                    template.setSubject(newSubject);
                                                                    database().updateMailTemplate(template);
                                                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                            messages().templateEditSubjectSuccess,
                                                                            Placeholder.component("template", miniMessage().deserialize(newSubject)),
                                                                            Placeholder.component("old_subject", miniMessage().deserialize(template.subject())),
                                                                            Placeholder.component("new_subject", miniMessage().deserialize(newSubject))
                                                                    ));
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
                                                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                            messages().templateEditContentSuccess,
                                                                            Placeholder.component("template", miniMessage().deserialize(template.subject()))
                                                                    ));
                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("sender")
                                                        .then(Commands.argument("new_sender", new MailTemplateArgumentType())
                                                                .executes(context -> {
                                                                    var template = context.getArgument("template", MailTemplate.class);
                                                                    var newSender = context.getArgument("new_sender", User.class);
                                                                    template.setSender(newSender);
                                                                    database().updateMailTemplate(template);
                                                                    context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                            messages().templateEditSenderSuccess,
                                                                            Placeholder.component("template", miniMessage().deserialize(template.subject())),
                                                                            Placeholder.component("sender", miniMessage().deserialize(newSender.name()))
                                                                    ));
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
                                                                                messages().templateNoAttachments,
                                                                                Placeholder.component("template", miniMessage().deserialize(template.subject()))
                                                                        ));
                                                                    } else {
                                                                        context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                messages().templateAttachmentList,
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
                                                                                        messages().invalidAttachmentIndex,
                                                                                        Placeholder.component("index", Component.text(index + 1))
                                                                                ));
                                                                                return Command.SINGLE_SUCCESS;
                                                                            }
                                                                            var attachment = attachments.get(index);
                                                                            database().deleteTemplateAttachment(template, attachment);
                                                                            context.getSource().getSender().sendMessage(miniMessage().deserialize(
                                                                                    messages().templateAttachmentDeleteSuccess,
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
                                        throw ERROR_PLAYER_REQUIRED.create();
                                    var user = database().getUser(player.getUniqueId());
                                    if (user == null)
                                        throw new IllegalStateException("User not found");
                                    new InboxMenu(player, user).open();
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.literal("of")
                                        .requires(source -> source.getSender().hasPermission("mailbox.inbox.others"))
                                        .then(Commands.argument("source", MailUserArgumentType.create())
                                                .executes(context -> {
                                                    if (!(context.getSource().getExecutor() instanceof Player player))
                                                        throw ERROR_PLAYER_REQUIRED.create();
                                                    var user = context.getArgument("source", User.class);
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
                        throw ERROR_PLAYER_REQUIRED.create();
                    new SendMailMenu(player).open();
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("to", MailUserArgumentType.withPermission())
                        .executes(context -> {
                            if (!(context.getSource().getExecutor() instanceof Player player))
                                throw ERROR_PLAYER_REQUIRED.create();
                            var user = context.getArgument("to", User.class);
                            if (player.getUniqueId().equals(user.id()) && !player.hasPermission("mailbox.admin"))
                                throw ERROR_CANNOT_SEND_YOURSELF.create();
                            new CreateMailMenu(player, user).open();
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build());
    }

    private static <A extends ArgumentBuilder<CommandSourceStack, A>> A addAttachmentNode(A node) {
        return node.then(Commands.literal("type")
                .then(Commands.literal("item")
                        .requires(context -> context.getSender().hasPermission("mailbox.attachment.item"))
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
                        .requires(context -> context.getSender().hasPermission("mailbox.attachment.command"))
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
                        .requires(context -> context.getSender().hasPermission("mailbox.attachment.vault"))
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
                        .requires(context -> context.getSender().hasPermission("mailbox.attachment.message"))
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
            if (template == null)
                throw new IllegalStateException("Template argument not found");
            var attachment = attachmentGetter.apply(context).build();
            if (!(attachment instanceof IAttachment<?, ?> iAttachment))
                throw new IllegalStateException("Built attachment is not an instance of IAttachment");
            database().createTemplateAttachment(template, iAttachment);
            context.getSource().getSender().sendMessage(miniMessage().deserialize(
                    messages().templateAttachmentAddSuccess,
                    Placeholder.component("template", miniMessage().deserialize(template.subject())),
                    Placeholder.component("attachment", attachment.format())
            ));
            return Command.SINGLE_SUCCESS;
        });
    }

}
