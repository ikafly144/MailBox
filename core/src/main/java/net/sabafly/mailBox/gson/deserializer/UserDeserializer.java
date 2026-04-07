package net.sabafly.mailBox.gson.deserializer;

import com.google.gson.*;
import net.sabafly.mailBox.mail.DummyMailUser;
import net.sabafly.mailBox.mail.PlayerMailUser;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Type;

public class UserDeserializer implements JsonDeserializer<User>, JsonSerializer<User> {
    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!typeOfT.getTypeName().equals(User.class.getTypeName())) {
            return new GsonBuilder()
                    .registerTypeAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                    .registerTypeAdapter(PlayerMailUser.class, new PlayerUserDeserializer())
                    .create()
                    .fromJson(json, typeOfT);
        }
        JsonObject jsonObject = json.getAsJsonObject();
        var typeObj = jsonObject.get("type");
        if (typeObj == null) {
            return null;
        }
        String type = typeObj.getAsString();
        return switch (type) {
            case "player" -> context.deserialize(jsonObject.get("data"), PlayerMailUser.class);
            case "dummy" -> context.deserialize(jsonObject.get("data"), DummyMailUser.class);
            default -> throw new JsonParseException("Unknown user type: " + type);
        };
    }

    public static <U extends User> JsonElement getDefaultUserJson(Class<U> userClass) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", getTypeName(userClass));
        return jsonObject;
    }

    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        var element = new GsonBuilder()
                .registerTypeAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                .registerTypeAdapter(PlayerMailUser.class, new PlayerUserDeserializer())
                .create()
                .toJsonTree(src);
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Unexpected element type");
        }
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("type", getTypeName(src.getClass()));
        wrapper.add("data", element);
        return wrapper;
    }

    private static String getTypeName(Class<? extends User> userClass) {
        if (PlayerMailUser.class.isAssignableFrom(userClass)) {
            return "player";
        } else if (DummyMailUser.class.isAssignableFrom(userClass)) {
            return "dummy";
        } else {
            throw new IllegalArgumentException("Unknown user class: " + userClass.getSimpleName());
        }
    }

}
