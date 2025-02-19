package net.sabafly.mailBox.mail;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record MailUser(@NotNull UUID uuid) {

    @Override
    public String toString() {
        return uuid.toString();
    }
}
