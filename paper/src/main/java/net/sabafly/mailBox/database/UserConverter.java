package net.sabafly.mailBox.database;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import net.kyori.adventure.key.Key;
import net.sabafly.mailBox.gson.deserializer.OfflinePlayerDeserializer;
import net.sabafly.mailBox.mail.DummyMailUser;
import net.sabafly.mailBox.mail.PlayerMailUser;
import net.sabafly.mailBox.mail.PluginMailUser;
import net.sabafly.mailbox.api.mail.User;
import org.apache.commons.lang3.NotImplementedException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UserConverter {

    @SuppressWarnings("unchecked")
    public static @Nullable <U extends User> U readUser(@NotNull UUID uuid, @NotNull Reader reader, @Nullable Key key) {
        BaseUser<U> baseUser = new GsonBuilder()
                .registerTypeHierarchyAdapter(BaseUser.class, new BaseUser.UserDeserializer())
                .create()
                .fromJson(reader, BaseUser.class);
        if (baseUser == null) {
            return null;
        }
        return baseUser.toUser(uuid, key);
    }

    public static byte @NotNull [] toJson(@NotNull User user) {
        BaseUser<?> baseUser = switch (user) {
            case PlayerMailUser playerMailUser -> new PlayerUser(playerMailUser.player());
            case PluginMailUser pluginMailUser -> new PluginUser(pluginMailUser.namespace(), pluginMailUser.name(), pluginMailUser.skinId());
            case DummyMailUser dummyMailUser -> new DummyUser(dummyMailUser.name(), dummyMailUser.skinId());
            default -> throw new IllegalArgumentException("Unknown user type: " + user.getClass().getSimpleName());
        };
        String json = new GsonBuilder()
                .registerTypeHierarchyAdapter(BaseUser.class, new BaseUser.UserDeserializer())
                .create()
                .toJson(baseUser);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    static abstract class BaseUser<U extends User> {

        enum UserType {
            dummy(DummyUser.class),
            player(PlayerUser.class),
            plugin(PluginUser.class);

            UserType(Class<? extends BaseUser<?>> clazz) {
                this.clazz = clazz;
            }

            private final Class<? extends BaseUser<?>> clazz;
        }

        BaseUser() {
        }

        public abstract @NotNull UserType type();

        public abstract U toUser(@NotNull UUID uuid, @Nullable Key key);

        protected abstract @Nullable Key defaultKey();

        static class UserDeserializer implements JsonDeserializer<BaseUser<?>>, JsonSerializer<BaseUser<?>> {

            private Gson createChildGson() {
                return new GsonBuilder()
                        .registerTypeHierarchyAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                        .registerTypeAdapter(PlayerUser.class, new PlayerUser.PlayerUserDeserializer())
                        .registerTypeAdapter(DummyUser.class, new DummyUser.DummyUserDeserializer())
                        .registerTypeAdapter(PluginUser.class, new PluginUser.PluginUserDeserializer())
                        .create();
            }

            @Override
            public BaseUser<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var child = createChildGson();
                var type = child.fromJson(json.getAsJsonObject().get("type"), UserType.class);
                return child.fromJson(json.getAsJsonObject().get("data"), type.clazz);
            }

            @Override
            public JsonElement serialize(BaseUser<?> src, Type typeOfSrc, JsonSerializationContext context) {
                var child = createChildGson();
                var element = child.toJsonTree(src);
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException("Unexpected element type");
                }
                JsonObject wrapper = new JsonObject();
                wrapper.addProperty("type", src.type().name());
                wrapper.add("data", element);
                return wrapper;
            }
        }
    }

    static class PlayerUser extends BaseUser<PlayerMailUser> {
        public final @NotNull OfflinePlayer player;

        PlayerUser(@NotNull OfflinePlayer player) {
            this.player = player;
        }

        @Override
        public @NotNull UserType type() {
            return UserType.player;
        }

        @Override
        public PlayerMailUser toUser(@NotNull UUID uuid, @Nullable Key key) {
            var defaultKey = defaultKey();
            if (defaultKey == null && key == null) {
                throw new IllegalStateException("Cannot determine key for player user");
            }
            return new PlayerMailUser(this.player, defaultKey != null ? defaultKey : key);
        }

        @SuppressWarnings("PatternValidation")
        @Override
        protected final @Nullable Key defaultKey() {
            var name = this.player.getName();
            if (name == null) {
                return null;
            }
            return Key.key("minecraft", User.sanitizeName(name));
        }

        static class PlayerUserDeserializer implements JsonDeserializer<PlayerUser> {
            @Override
            public PlayerUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var child = new GsonBuilder()
                        .registerTypeHierarchyAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                        .create();
                var jsonObj = json.getAsJsonObject();
                if (jsonObj == null) throw new JsonParseException("Expected JsonObject");
                var player = child.fromJson(jsonObj.get("player"), OfflinePlayer.class);
                if (player == null) throw new JsonParseException("Player cannot be null");
                return new PlayerUser(player);
            }
        }

    }

    static class DummyUser extends BaseUser<DummyMailUser> {
        public final @NotNull String name;
        public final @Nullable String skinId;

        DummyUser(@NotNull String name, @Nullable String skinId) {
            Preconditions.checkArgument(!name.isBlank(), "Name cannot be blank");
            this.name = name;
            this.skinId = skinId;
        }

        @Override
        public @NotNull UserType type() {
            return UserType.dummy;
        }

        @SuppressWarnings("PatternValidation")
        @Override
        public DummyMailUser toUser(@NotNull UUID uuid, @Nullable Key key) {
            var defaultKey = defaultKey();
            return DummyMailUser.createUser(uuid, this.name, defaultKey.value(), this.skinId);
        }

        @Override
        protected @NotNull Key defaultKey() {
            return Key.key("mailbox", User.sanitizeName(this.name));
        }

        static class DummyUserDeserializer implements JsonDeserializer<DummyUser> {
            @Override
            public DummyUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var jsonObj = json.getAsJsonObject();
                if (jsonObj == null) throw new JsonParseException("Expected JsonObject");
                var name = jsonObj.get("name").getAsString();
                if (name == null) throw new JsonParseException("Name cannot be null for dummy user");
                var skinId = jsonObj.get("skin_id").getAsString();
                return new DummyUser(name, skinId);
            }
        }

    }

    static class PluginUser extends BaseUser<PluginMailUser> {
        public final @NotNull String namespace;
        public final @NotNull String name;
        public final @Nullable String skinId;

        PluginUser(@NotNull String namespace, @NotNull String name, @Nullable String skinId) {
            Preconditions.checkArgument(!namespace.isBlank(), "Namespace cannot be blank");
            Preconditions.checkArgument(!name.isBlank(), "Name cannot be blank");
            this.namespace = namespace;
            this.name = name;
            this.skinId = skinId;
        }

        @Override
        public @NotNull UserType type() {
            return UserType.plugin;
        }

        @Override
        public PluginMailUser toUser(@NotNull UUID uuid, @Nullable Key key) {
            if (key == null) throw new IllegalStateException("Cannot determine key for plugin user");
            if (!key.namespace().equals(namespace))
                throw new IllegalStateException("Key namespace does not match plugin user namespace");
            var plugin = Bukkit.getPluginManager().getPlugin(namespace);
            if (plugin == null) throw new IllegalStateException("Plugin not found for namespace: " + namespace);
            return PluginMailUser.createPlugin(uuid, name, plugin, skinId);
        }

        @Override
        protected @Nullable Key defaultKey() {
            throw new NotImplementedException("Default key for plugin user is not implemented");
        }

        static class PluginUserDeserializer implements JsonDeserializer<PluginUser> {
            @Override
            public PluginUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var jsonObj = json.getAsJsonObject();
                if (jsonObj == null) throw new JsonParseException("Expected JsonObject");
                var namespace = jsonObj.get("namespace").getAsString();
                if (namespace == null) throw new JsonParseException("Namespace cannot be null for plugin user");
                var name = jsonObj.get("name").getAsString();
                if (name == null) throw new JsonParseException("Name cannot be null for plugin user");
                var skinId = jsonObj.get("skin_id").getAsString();
                return new PluginUser(namespace, name, skinId);
            }
        }

    }

}
