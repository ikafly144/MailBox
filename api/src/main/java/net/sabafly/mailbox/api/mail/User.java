package net.sabafly.mailbox.api.mail;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface User {

    @NotNull UUID id();

    @NotNull String name();

    @NotNull Key key();

}
