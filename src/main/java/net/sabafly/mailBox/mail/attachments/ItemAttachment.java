package net.sabafly.mailBox.mail.attachments;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

import static net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText;

@SuppressWarnings("UnstableApiUsage")
public class ItemAttachment extends BaseAttachment<ItemAttachment> {

    @NotNull
    private final ItemStack itemStack;

    public ItemAttachment(@NotNull ItemStack itemStack, boolean received, @Nullable LocalDateTime expireTime) {
        super(plainText().serialize(getEffectiveName(itemStack)), Type.ITEM, received, RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(itemStack.getType().key()), expireTime);
        this.itemStack = itemStack;
    }

    protected ItemAttachment(@NotNull UUID id, @NotNull ItemStack itemStack, boolean received, @Nullable LocalDateTime expireTime) {
        super(id, plainText().serialize(getEffectiveName(itemStack)), Type.ITEM, received, RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).getOrThrow(itemStack.getType().key()), expireTime);
        this.itemStack = itemStack;
    }

    private static Component getEffectiveName(@NotNull ItemStack itemStack) {
        return itemStack.effectiveName().append(itemStack.getAmount() > 1 ? Component.text(" ×" + itemStack.getAmount()) : Component.empty());
    }

    @Override
    protected @NotNull ItemType defaultPreviewType() {
        return ItemType.BUNDLE;
    }

    @Override
    public String toString() {
        return "ItemAttachment{" +
                "name='" + getName() + '\'' +
                '}';
    }

    @Override
    public void apply(@NotNull Player player) {
        player.getInventory().addItem(itemStack.clone()).forEach((index, item) -> player.getWorld().dropItem(player.getLocation(), item));
    }

    @Override
    public void cancel(@NotNull Player player) {
        player.getInventory().addItem(itemStack.clone()).forEach((index, item) -> player.getWorld().dropItem(player.getLocation(), item));
    }

    @Override
    public byte @NotNull [] serialize() {
        return itemStack.serializeAsBytes();
    }

    public static @NotNull ItemAttachment deserialize(@NotNull UUID id, @NotNull String ignoredName, boolean received, byte @NotNull [] data, @Nullable ItemType ignoredItemType, @Nullable LocalDateTime expireTime) {
        return new ItemAttachment(id, ItemStack.deserializeBytes(data), received, expireTime);
    }

    @Override
    public @NotNull ItemAttachment create(boolean received) {
        return new ItemAttachment(itemStack, received, getExpireTime().orElse(null));
    }
}
