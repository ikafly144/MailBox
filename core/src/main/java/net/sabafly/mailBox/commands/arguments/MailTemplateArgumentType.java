package net.sabafly.mailBox.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.sabafly.mailBox.mail.MailTemplate;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.sabafly.mailBox.MailBox.database;

public class MailTemplateArgumentType implements CustomArgumentType<MailTemplate, String> {

    private static final DynamicCommandExceptionType NO_SUCH_TEMPLATE_EXCEPTION = new DynamicCommandExceptionType(
            input -> () -> "No mail template found with id or title: " + input
    );

    @Override
    public @NonNull MailTemplate parse(@NonNull StringReader reader) throws CommandSyntaxException {
        var input = getNativeType().parse(reader);
        try {
            var id =UUID.fromString(input);
            var template = database().getMailTemplate(id).orElse(null);
            if (template != null) return template;
        } catch (IllegalArgumentException ignored) {
        }
        var result = database().getAllMailTemplates().stream()
                .filter(t -> t.title().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
        if (result == null) {
            throw NO_SUCH_TEMPLATE_EXCEPTION.create(input);
        }
        return result;
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        var input = builder.getRemainingLowerCase();
        database().getAllMailTemplates().stream()
                .map(MailTemplate::title)
                .filter(title -> title.toLowerCase().startsWith(input))
                .map(StringArgumentType::escapeIfRequired)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
