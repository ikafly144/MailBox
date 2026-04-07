package net.sabafly.mailBox.mail;

import net.sabafly.mailbox.api.mail.User;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.sabafly.mailBox.MailBox.config;

public class DummyMailUser implements User {

    public static final UUID SYSTEM_UUID = new UUID(0, 0);

    @Contract("_, _ -> new")
    public static @NotNull DummyMailUser createUser(@NotNull UUID uuid, @NotNull String name) {
        return new DummyMailUser(uuid, name);
    }

    @Contract("_ -> new")
    public static @NotNull DummyMailUser createUser(@NotNull String name) {
        return new DummyMailUser(UUID.randomUUID(), name);
    }

    private final UUID uuid;
    private final String name;

    private DummyMailUser(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public @NotNull UUID id() {
        return uuid;
    }

    @Override
    public @NotNull String name() {
        if (uuid.equals(SYSTEM_UUID)) {
            return config().messages.systemName;
        }
        return name;
    }

}
