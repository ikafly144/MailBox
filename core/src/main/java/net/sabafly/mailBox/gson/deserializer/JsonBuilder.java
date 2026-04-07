package net.sabafly.mailBox.gson.deserializer;

import com.google.gson.GsonBuilder;
import net.sabafly.mailbox.api.mail.User;
import org.bukkit.OfflinePlayer;

public class JsonBuilder {

    public static GsonBuilder builder() {
        return new GsonBuilder()
                .registerTypeHierarchyAdapter(User.class, new UserDeserializer())
                .registerTypeHierarchyAdapter(OfflinePlayer.class, new OfflinePlayerDeserializer());
    }

    private JsonBuilder() {
    }

}
