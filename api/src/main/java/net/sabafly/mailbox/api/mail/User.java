package net.sabafly.mailbox.api.mail;

import com.google.common.base.Preconditions;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.net.URL;
import java.util.UUID;

public interface User extends Keyed {

    @NotNull UUID id();

    @NotNull String name();

    @SuppressWarnings("PatternValidation")
    static @NonNull @KeyPattern.Value String sanitizeName(@NotNull String name) {
        final var newName = name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        Preconditions.checkArgument(Key.parseableValue(newName), "Generated key value does not match pattern: " + newName);
        return newName;
    }

    @Nullable URL skinUrl();

}
