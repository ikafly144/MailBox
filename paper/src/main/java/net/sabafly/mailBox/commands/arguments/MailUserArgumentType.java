package net.sabafly.mailBox.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailbox.api.mail.User;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

import static net.sabafly.mailBox.MailBox.database;

public class MailUserArgumentType implements CustomArgumentType<User, Key> {

    private static final DynamicCommandExceptionType NO_SUCH_USER_EXCEPTION = new DynamicCommandExceptionType(
            input -> () -> "No such user: " + input
    );

    @Override
    public @NonNull User parse(@NonNull StringReader reader) throws CommandSyntaxException {
        var key = getNativeType().parse(reader);
        var user = database().getUserByAddress(key);
        if (user == null) {
            throw NO_SUCH_USER_EXCEPTION.create(key);
        }
        return user;
    }

    @Override
    public <S> @NonNull User parse(@NonNull StringReader reader, @NonNull S source) throws CommandSyntaxException {
        var c = reader.getCursor();
        var key = getNativeType().parse(reader);
        reader.setCursor(c);
        if (!key.namespace().equals(Key.MINECRAFT_NAMESPACE) && source instanceof CommandSourceStack stack) {
            var sender = stack.getSender();
            if ((!sender.hasPermission("mailbox.mailto.namespace.*") &&
                sender.permissionValue("mailbox.mailto.namespace." + key.namespace()) != TriState.TRUE) ||
                sender.permissionValue("mailbox.mailto.user." + key.asMinimalString()) == TriState.FALSE) {
                throw NO_SUCH_USER_EXCEPTION.create(key);
            }
        }
        return CustomArgumentType.super.parse(reader, source);
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        database().getAllUsers().stream()
                .map(User::key)
                .filter(key ->
                        key.namespace().equals(Key.MINECRAFT_NAMESPACE) ||
                        context.getSource() instanceof CommandSourceStack source &&
                        (source.getSender().hasPermission("mailbox.mailto.namespace.*") ||
                         source.getSender().permissionValue("mailbox.mailto.namespace." + key.namespace()) == TriState.TRUE) &&
                        source.getSender().permissionValue("mailbox.mailto.user." + key.asMinimalString()) != TriState.FALSE
                )
                .map(Key::asMinimalString)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    @Override
    public @NonNull ArgumentType<Key> getNativeType() {
        return ArgumentTypes.key();
    }
}
