package net.sabafly.mailBox.gson.deserializer;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.UUID;

public class OfflinePlayerDeserializer implements JsonDeserializer<OfflinePlayer>, JsonSerializer<OfflinePlayer> {

    @Override
    public OfflinePlayer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        final UUID uuid;
        try {
            uuid = UUID.fromString(json.getAsString());
        } catch (IllegalArgumentException exception) {
            throw new JsonParseException("Invalid UUID format: " + json.getAsString(), exception);
        }
        return Optional.of(Bukkit.getOfflinePlayer(uuid)).filter(OfflinePlayer::hasPlayedBefore).orElse(null);
    }

    @Override
    public JsonElement serialize(OfflinePlayer src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getUniqueId().toString());
    }
}
