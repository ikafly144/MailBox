package net.sabafly.mailbox.type;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.sabafly.mailbox.configurations.PluginConfiguration;
import net.sabafly.mailbox.utils.MailUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MailBoxEntry {

    public static final NamespacedKey KEY = new NamespacedKey("mailbox", "entry");

    public static MailBoxEntry create(@NotNull ItemStack itemStack, @NotNull Component name, @NotNull Component description, @NotNull MailAction action) {
        return new MailBoxEntry(itemStack, name, description, action);
    }

    public static MailBoxEntry create(@NotNull ItemStack itemStack, @NotNull Component name, @NotNull Component description, @NotNull MailAction action, LocalDateTime received) {
        return new MailBoxEntry(itemStack, name, description, action, received);
    }

    @Getter
    @Nullable
    private final LocalDateTime received;
    private final ItemStack itemStack;
    @Getter
    private final Component name;
    @Getter
    private final Component description;
    @Getter
    private final MailAction action;

    private MailBoxEntry(ItemStack itemStack, Component name, Component description, MailAction action) {
        this(itemStack, name, description, action, LocalDateTime.now().withNano(0));
    }

    private MailBoxEntry(ItemStack itemStack, Component name, Component description, MailAction action, @Nullable LocalDateTime received) {
        this.itemStack = itemStack;
        this.received = received;
        this.name = name;
        this.description = description.color(NamedTextColor.GRAY);
        this.action = action;
    }

    public boolean isValid() {
        return this.itemStack != null && this.name != null && this.description != null && this.action != null;
    }

    public ItemStack getItemStack() {
        this.itemStack.editMeta(itemMeta -> {
            itemMeta.displayName(this.name);
            var list = new ArrayList<Component>();
            list.add(Component.text().color(NamedTextColor.GRAY).content(PluginConfiguration.get().messages.receivedTime +": %s".formatted(this.received != null ? this.received.withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "[NO DATA]")).build());
            list.addAll(description.children());
            itemMeta.lore(list);
        });
        return this.itemStack;
    }

    public void send(Player player) {
        MailUtil.sendMail(player, this);
    }
}
