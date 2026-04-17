package net.sabafly.mailBox.mail;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.sabafly.mailBox.MailBox;
import net.sabafly.mailbox.api.mail.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import static net.sabafly.mailBox.MailBox.config;

public sealed class DummyMailUser implements User permits PluginMailUser {

    public static final UUID SYSTEM_UUID = new UUID(0, 0);
    @ApiStatus.Internal
    public static final DummyMailUser SYSTEM_USER = createUser(SYSTEM_UUID, config().messages.systemName, "system", "40b05e699d28b3a278a92d169dca9d57c0791d07994d82de3f9ed4a48afe0e1d");

    @Contract("_, _, _, _ -> new")
    @ApiStatus.Internal
    public static @NotNull DummyMailUser createUser(
            @NotNull UUID uuid,
            @NotNull String name,
            @NotNull @KeyPattern.Value String keyValue,
            @Nullable String skinId
    ) {
        return new DummyMailUser(uuid, name, keyValue, skinId);
    }

    @Contract("_, _, _ -> new")
    @ApiStatus.Internal
    public static @NotNull DummyMailUser createUser(
            @NotNull String name,
            @NotNull @KeyPattern.Value String keyValue,
            @Nullable String skinId
    ) {
        return new DummyMailUser(UUID.randomUUID(), name, keyValue, skinId);
    }

    private final UUID uuid;
    private final String name;
    private final Key key;

    private final String skinId;

    private DummyMailUser(@NotNull UUID uuid, @NotNull String name, @NotNull @KeyPattern.Value String keyValue, String skinId) {
        this(uuid, name, Key.key(MailBox.getInstance(), keyValue), skinId);
    }

    protected DummyMailUser(@NotNull UUID uuid, @NotNull String name, @NotNull Key key, String skinId) {
        this.uuid = uuid;
        this.name = name;
        this.key = key;
        this.skinId = skinId;
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

    public @Nullable String skinId() {
        return skinId;
    }

    @Override
    public @Nullable URL skinUrl() {
        if (skinId == null) return null;
        try {
            return URI.create("https://textures.minecraft.net/texture/" + skinId).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

}
