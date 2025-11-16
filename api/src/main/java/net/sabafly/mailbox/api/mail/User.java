package net.sabafly.mailbox.api.mail;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface User {

    @NotNull UUID id();

    @NotNull String name();

}
