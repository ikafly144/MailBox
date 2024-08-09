package net.sabafly.mailbox.type;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.sabafly.mailbox.utils.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public class MailAction {

    public static final MailAction NONE = of(ActionType.NONE, "");

    public static MailAction of(ActionType actionType, String value) {
        return new MailAction(actionType, value);
    }

    private MailAction(ActionType actionType, String value) {
        this.action = actionType;
        this.value = value;
    }

    public MailAction() {}

    @SerializedName("action")
    @Expose
    public ActionType action = ActionType.NONE;
    @SerializedName("value")
    @Expose
    public String value = "";
    @SerializedName("used")
    @Expose
    public boolean used = false;

    public void doAction(Player player) {
        if (!used && action != null) action.consumer.accept(player, value);
        used = true;
    }

    public enum ActionType {
        @SerializedName("none")
        NONE((player, value) -> {
        }),
        @SerializedName("close")
        CLOSE((player, value) -> player.closeInventory()),
        @SerializedName("reward_item")
        REWARD_ITEM((player, value) -> {
            ItemStack item = ItemUtil.parseItem(value);
            if (item == null || item.isEmpty()) {
                return;
            }
            var leftOver = player.getInventory().addItem(item);
            if (!leftOver.isEmpty()) {
                leftOver.forEach((integer, itemStack) -> player.getWorld().dropItem(player.getLocation(), itemStack, it -> it.setOwner(player.getUniqueId())));
            }
        }),
        @SerializedName("run_command")
        RUN_COMMAND((player, value) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as %s run %s".formatted(player.getName(), value.startsWith("/") ? value.substring(1) : value)))
        ;
        private final BiConsumer<Player, String> consumer;
        ActionType(BiConsumer<Player, String> consumer) {
            this.consumer = consumer;
        }
    }

    public static String serialize(MailAction mailAction) {
        Gson gson = new Gson();
        return gson.toJson(mailAction);
    }

    public static MailAction deserialize(String string) {
        Gson gson = new Gson();
        return gson.fromJson(string, MailAction.class);
    }

}
