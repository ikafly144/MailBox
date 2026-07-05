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
import org.bukkit.permissions.Permissible;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

import static net.sabafly.mailBox.MailBox.database;

public class MailUserArgumentType implements CustomArgumentType<User, Key> {

    private static final DynamicCommandExceptionType NO_SUCH_USER_EXCEPTION = new DynamicCommandExceptionType(
            input -> () -> "No such user: " + input
    );

    public static MailUserArgumentType create() {
        return new MailUserArgumentType(false);
    }

    public static MailUserArgumentType withPermission() {
        return new MailUserArgumentType(true);
    }

    private final boolean perm;

    private MailUserArgumentType(boolean perm) {
        this.perm = perm;
    }

    @Override
    public @NonNull User parse(@NonNull StringReader reader) throws CommandSyntaxException {
        @SuppressWarnings("PatternValidation") var key = Key.key(reader.readUnquotedString().toLowerCase());
        var user = database().getUserByAddress(key);
        if (user == null) {
            throw NO_SUCH_USER_EXCEPTION.create(key);
        }
        return user;
    }

    @Override
    public <S> @NonNull User parse(@NonNull StringReader reader, @NonNull S source) throws CommandSyntaxException {
        var c = reader.getCursor();
        @SuppressWarnings("PatternValidation") var key = Key.key(reader.readUnquotedString().toLowerCase());
        reader.setCursor(c);
        if (source instanceof CommandSourceStack stack) {
            var sender = stack.getSender();
            if (perm && !checkPermission(sender, key)) {
                throw NO_SUCH_USER_EXCEPTION.create(key);
            }
        }
        return CustomArgumentType.super.parse(reader, source);
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        database().getAllUsers().stream()
                .map(User::key)
                .filter(key -> perm || context.getSource() instanceof CommandSourceStack source && checkPermission(source.getSender(), key))
                .map(Key::asMinimalString)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static boolean checkPermission(@NonNull Permissible subject, @NonNull Key key) {
        final var userPerm = subject.permissionValue("mailbox.mailto.user." + key.asMinimalString());
        final var namespacePerm = subject.permissionValue("mailbox.mailto.namespace." + key.namespace());
        return userPerm == TriState.TRUE || namespacePerm == TriState.TRUE && userPerm != TriState.FALSE;
    }

    @Override
    public @NonNull ArgumentType<Key> getNativeType() {
        return ArgumentTypes.key();
    }
}
