package net.sabafly.mailbox.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class DateArgumentType implements CustomArgumentType<LocalDateTime, String> {

    @Override
    public @NotNull LocalDateTime parse(@NotNull StringReader reader) throws CommandSyntaxException {
        return LocalDateTime.parse(reader.readString());
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.string();
    }

    @Override
    public @NotNull <S> CompletableFuture<Suggestions> listSuggestions(@NotNull CommandContext<S> context, @NotNull SuggestionsBuilder builder) {
        var remain = builder.getRemaining();
        if (remain.isEmpty()) {
            // 現在の日時を提案 (例: "2021-01-01T00:00:00")
            // ミリ秒は省略
            builder.suggest("\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).substring(0, 19) + "\"");
            return builder.buildFuture();
        }
        // 正規表現で年が入力されているかどうかを判定
        if (remain.matches("^([\"'])?\\d{4}")) {
            builder.suggest(remain + "-");
            return builder.buildFuture();
        }
        // 正規表現で月が入力されているかどうかを判定
        if (remain.matches("^([\"'])?\\d{4}-\\d{2}")) {
            builder.suggest(remain + "-");
            return builder.buildFuture();
        }
        // 正規表現で日が入力されているかどうかを判定
        if (remain.matches("^([\"'])?\\d{4}-\\d{2}-\\d{2}")) {
            builder.suggest(remain + "T");
            return builder.buildFuture();
        }
        // 正規表現で時が入力されているかどうかを判定
        if (remain.matches("^([\"'])?\\d{4}-\\d{2}-\\d{2}T\\d{2}")) {
            builder.suggest(remain + ":");
            return builder.buildFuture();
        }
        // 正規表現で分が入力されているかどうかを判定
        if (remain.matches("^([\"'])?\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
            builder.suggest(remain + ":");
            return builder.buildFuture();
        }
        // 正規表現で秒が入力されているかどうかを判定
        if (remain.matches("^([\"'])?\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
            // クオーテーションが入力されているかどうかを判定
            if (remain.startsWith("\"")) {
                builder.suggest(remain + "\"");
            } else if (remain.startsWith("'")) {
                builder.suggest(remain + "'");
            } else {
                builder.suggest(remain);
            }
            return builder.buildFuture();
        }
        return builder.restart().buildFuture();
    }
}
