package net.sabafly.mailBox.gson.deserializer;

import com.google.gson.*;
import net.sabafly.mailBox.mail.PlayerMailUser;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Type;

public class PlayerUserDeserializer implements JsonDeserializer<PlayerMailUser>, JsonSerializer<PlayerMailUser> {
    @Override
    public PlayerMailUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        var playerObj = jsonObject.get("player");
        if (playerObj == null) {
            throw new JsonParseException("Missing 'player' field");
        }
        OfflinePlayer player = new GsonBuilder()
                .registerTypeHierarchyAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                .create()
                .fromJson(playerObj, OfflinePlayer.class);
        if (player == null) {
            throw new JsonParseException("Player not found: " + playerObj.getAsString());
        }
        return new PlayerMailUser(player);
    }

    @Override
    public JsonElement serialize(PlayerMailUser src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("player", new GsonBuilder()
                .registerTypeHierarchyAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer())
                .create()
                .toJsonTree(src.offlinePlayer()));
        return jsonObject;
    }
}
