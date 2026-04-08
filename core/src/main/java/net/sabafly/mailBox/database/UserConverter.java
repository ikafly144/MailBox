package net.sabafly.mailBox.database;

import com.google.gson.*;
import net.kyori.adventure.key.Key;
import net.sabafly.mailBox.gson.deserializer.OfflinePlayerDeserializer;
import net.sabafly.mailBox.mail.DummyMailUser;
import net.sabafly.mailBox.mail.PlayerMailUser;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

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
            case PlayerMailUser playerMailUser ->
                    new PlayerUser(BaseUser.UserType.player, playerMailUser.offlinePlayer());
            case DummyMailUser dummyMailUser ->
                    new DummyUser(BaseUser.UserType.dummy, dummyMailUser.id(), dummyMailUser.name());
            default -> throw new IllegalArgumentException("Unknown user type: " + user.getClass().getSimpleName());
        };
        String json = new GsonBuilder()
                .registerTypeHierarchyAdapter(BaseUser.class, new BaseUser.UserDeserializer())
                .create()
                .toJson(baseUser);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    static abstract class BaseUser<U extends User> {
        public final @NotNull UserType type;

        enum UserType {
            player(PlayerUser.class),
            dummy(DummyUser.class);

            UserType(Class<? extends BaseUser<?>> clazz) {
                this.clazz = clazz;
            }

            private final Class<? extends BaseUser<?>> clazz;
        }

        BaseUser(@NotNull UserType userType) {
            this.type = userType;
        }

        @SuppressWarnings({"unchecked", "PatternValidation"})
        public U toUser(@NotNull UUID uuid, @Nullable Key key) {
            var defaultKey = defaultKey();
            if (defaultKey == null && key == null) {
                throw new IllegalStateException("Cannot determine key for user");
            }
            return (U) switch (this.type) {
                case dummy -> {
                    var dummyUser = (DummyUser) this;
                    yield DummyMailUser.createUser(uuid, dummyUser.name, (defaultKey != null ? defaultKey : key).value());
                }
                case player -> {
                    var playerUser = (PlayerUser) this;
                    yield new PlayerMailUser(playerUser.offlinePlayer, defaultKey != null ? defaultKey : key);
                }
                default -> throw new IllegalStateException("Unexpected user type: " + this.type);
            };
        }

        @SuppressWarnings("PatternValidation")
        private @Nullable Key defaultKey() {
            switch (this.type) {
                case dummy -> {
                    var dummyUser = (DummyUser) this;
                    return Key.key("mailbox", sanitizeName(dummyUser.name));
                }
                case player -> {
                    var playerUser = (PlayerUser) this;
                    var name = playerUser.offlinePlayer.getName();
                    if (name == null) {
                        return null;
                    }
                    return Key.key("minecraft", sanitizeName(name));
                }
                default -> throw new IllegalStateException("Unexpected user type: " + this.type);
            }
        }

        private @NonNull String sanitizeName(@NotNull String name) {
            return name.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        }

        static class UserDeserializer implements JsonDeserializer<BaseUser<?>>, JsonSerializer<BaseUser<?>> {

            private Gson createChildGson() {
                return new GsonBuilder()
                        .registerTypeAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                        .registerTypeAdapter(PlayerUser.class, new PlayerUser.PlayerUserDeserializer())
                        .registerTypeAdapter(DummyUser.class, new DummyUser.DummyUserDeserializer())
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
                wrapper.addProperty("type", src.type.name());
                wrapper.add("data", element);
                return wrapper;
            }
        }
    }

    static class PlayerUser extends BaseUser<PlayerMailUser> {
        public final @NotNull OfflinePlayer offlinePlayer;

        PlayerUser(@NotNull BaseUser.UserType userType, @NotNull OfflinePlayer offlinePlayer) {
            super(userType);
            this.offlinePlayer = offlinePlayer;
        }

        static class PlayerUserDeserializer implements JsonDeserializer<PlayerUser> {
            @Override
            public PlayerUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var child = new GsonBuilder()
                        .registerTypeAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                        .create();
                var jsonObj = json.getAsJsonObject();
                if (jsonObj == null) throw new JsonParseException("Expected JsonObject");
                var player = child.fromJson(jsonObj.get("player"), OfflinePlayer.class);
                if (player == null) throw new JsonParseException("Player cannot be null");
                return new PlayerUser(BaseUser.UserType.player, player);
            }
        }

    }

    static class DummyUser extends BaseUser<DummyMailUser> {
        public final @NotNull UUID uuid;
        public final @NotNull String name;

        DummyUser(@NotNull BaseUser.UserType type, @NotNull UUID uuid, @NotNull String name) {
            super(type);
            this.uuid = uuid;
            this.name = name;
        }

        static class DummyUserDeserializer implements JsonDeserializer<DummyUser> {
            @Override
            public DummyUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                var jsonObj = json.getAsJsonObject();
                if (jsonObj == null) throw new JsonParseException("Expected JsonObject");
                var uuid = UUID.fromString(jsonObj.get("uuid").getAsString());
                var name = jsonObj.get("name").getAsString();
                return new DummyUser(BaseUser.UserType.dummy, uuid, name);
            }
        }

    }

}
