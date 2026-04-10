package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

public class ItemAttachment extends BaseAttachment<ItemAttachment> {

    @NotNull
    private final ItemStack itemStack;

    public ItemAttachment(@NotNull ItemStack item, boolean received, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        super(miniMessage().serialize(getEffectiveName(item)), Type.ITEM, received, item.clone(), receivedTime, expireDuration);
        this.itemStack = item;
    }

    public ItemAttachment(@NotNull UUID id, @NotNull ItemStack item, boolean received, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        super(id, miniMessage().serialize(getEffectiveName(item)), Type.ITEM, received, item.clone(), receivedTime, expireDuration);
        this.itemStack = item;
    }

    private static Component getEffectiveName(@NotNull ItemStack itemStack) {
        return itemStack.effectiveName().append(itemStack.getAmount() > 1 ? Component.text(" ×" + itemStack.getAmount()) : Component.empty());
    }

    public @NotNull ItemStack content() {
        return itemStack.clone();
    }

    @Override
    public String toString() {
        return "ItemAttachment{" +
                "name='" + getName() + '\'' +
                '}';
    }

    @Override
    public void apply(@NotNull Player player) {
        player.give(content());
    }

    @Override
    public void cancel(@NotNull Player player) {
        player.give(content());
    }

    @Override
    public boolean checkRequirement(@NotNull Player player) {
        return player.getInventory().all(content()).values().stream().mapToInt(ItemStack::getAmount).sum() > content().getAmount();
    }

    @Override
    public boolean consumeRequirement(@NotNull Player player) {
        if (!checkRequirement(player)) return false;
        var removed = player.getInventory().removeItem(content()).values().stream().mapToInt(ItemStack::getAmount).sum();
        if (removed > content().getAmount()) {
            var retItem = content();
            retItem.setAmount(retItem.getAmount() - removed);
            player.give(retItem);
        }
        return true;
    }

    @Override
    public byte @NotNull [] serialize() {
        return itemStack.serializeAsBytes();
    }

    public static @NotNull ItemAttachment deserialize(@NotNull UUID id, @NotNull String ignoredName, boolean received, byte @NotNull [] data, @Nullable ItemStack ignoredPreviewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new ItemAttachment(id, ItemStack.deserializeBytes(data), received, receivedTime, expireDuration);
    }

    @Override
    public @NotNull ItemAttachment create(boolean opened) {
        return new ItemAttachment(itemStack, false, LocalDateTime.now(), expireDuration().orElse(config().mail.getExpirationDuration()));
    }
}
