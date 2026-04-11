package net.sabafly.mailBox.mail;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailbox.api.mail.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static net.sabafly.mailBox.MailBox.config;

public sealed class DummyMailUser implements User permits PluginMailUser {

    public static final UUID SYSTEM_UUID = new UUID(0, 0);
    @ApiStatus.Internal
    public static final DummyMailUser SYSTEM_USER = createUser(SYSTEM_UUID, config().messages.systemName, "system");

    @Contract("_, _, _ -> new")
    @ApiStatus.Internal
    public static @NotNull DummyMailUser createUser(@NotNull UUID uuid, @NotNull String name, @NotNull @KeyPattern.Value String keyValue) {
        return new DummyMailUser(uuid, name, keyValue);
    }

    @Contract("_, _ -> new")
    @ApiStatus.Internal
    public static @NotNull DummyMailUser createUser(@NotNull String name, @NotNull @KeyPattern.Value String keyValue) {
        return new DummyMailUser(UUID.randomUUID(), name, keyValue);
    }

    private final UUID uuid;
    private final String name;
    private final Key key;

    private DummyMailUser(@NotNull UUID uuid, @NotNull String name, @NotNull @KeyPattern.Value String keyValue) {
        this(uuid, name, Key.key(MailBox.getInstance(), keyValue));
    }

    protected DummyMailUser(@NotNull UUID uuid, @NotNull String name, @NotNull Key key) {
        this.uuid = uuid;
        this.name = name;
        this.key = key;
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

    @Override
    public @NotNull Key key() {
        return key;
    }

}
