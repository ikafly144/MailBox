package net.sabafly.mailBox.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.sabafly.mailBox.mail.MailTemplate;
import org.jspecify.annotations.NonNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.sabafly.mailBox.MailBox.database;

public class MailTemplateArgumentType implements CustomArgumentType<MailTemplate, String> {
    @Override
    public MailTemplate parse(@NonNull StringReader reader) throws CommandSyntaxException {
        var input = reader.readUnquotedString();
        try {
            var id =UUID.fromString(input);
            var template = database().getMailTemplate(id).orElse(null);
            if (template != null) return template;
        } catch (IllegalArgumentException ignored) {
        }
        return database().getAllMailTemplates().stream()
                .filter(t -> t.title().equalsIgnoreCase(input))
                .findFirst()
                .orElse(null);
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        var input = builder.getRemainingLowerCase();
        database().getAllMailTemplates().stream()
                .filter(t -> t.title().toLowerCase().startsWith(input))
                .forEach(t -> builder.suggest(t.title()));
        return builder.buildFuture();
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }
}
