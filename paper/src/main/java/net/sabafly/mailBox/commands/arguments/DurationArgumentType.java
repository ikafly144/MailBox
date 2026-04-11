package net.sabafly.mailBox.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class DurationArgumentType implements CustomArgumentType<Duration, String> {

    private static final SimpleCommandExceptionType ERROR_INVALID_UNIT = new SimpleCommandExceptionType(new AdventureComponent(Component.translatable("argument.time.invalid_unit")));
    private static final Dynamic2CommandExceptionType ERROR_TICK_COUNT_TOO_LOW = new Dynamic2CommandExceptionType(
            (value, limit) -> new AdventureComponent(miniMessage().deserialize(
                    "The duration must not be less than <limit>: found <value>",
                    Placeholder.parsed("value", String.valueOf(value)),
                    Placeholder.parsed("limit", String.valueOf(limit))
            ))
    );
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap<>();
    private final int minimum;

    static {
        UNITS.put("", 1);
        UNITS.put("s", 1);
        UNITS.put("m", 60);
        UNITS.put("h", 3600);
        UNITS.put("d", 86400);
    }

    public static DurationArgumentType duration() {
        return new DurationArgumentType(1);
    }

    private DurationArgumentType(int minimum) {
        this.minimum = minimum;
    }

    @Override
    public @NonNull Duration parse(@NonNull StringReader reader) throws CommandSyntaxException {
        float value = reader.readFloat();
        String unit = reader.readUnquotedString();
        int factor = UNITS.getOrDefault(unit, 0);
        if (factor == 0) {
            throw ERROR_INVALID_UNIT.createWithContext(reader);
        } else {
            int seconds = Math.round(value * (float) factor);
            if (seconds < this.minimum) {
                throw ERROR_TICK_COUNT_TOO_LOW.createWithContext(reader, seconds, this.minimum);
            } else {
                return Duration.ofSeconds(seconds);
            }
        }
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getRemaining());

        try {
            reader.readFloat();
        } catch (CommandSyntaxException var5) {
            return builder.buildFuture();
        }

        return SharedSuggestionProvider.suggest(UNITS.keySet(), builder.createOffset(builder.getStart() + reader.getCursor()));
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }
}
