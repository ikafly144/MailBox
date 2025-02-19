package net.sabafly.mailBox.mail.attachments;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class VaultValueAttachment extends BaseAttachment<VaultValueAttachment> {

    private final double value;

    public VaultValueAttachment(@NotNull String name, double value, @Nullable LocalDateTime expireTime) {
        super(name, Type.VAULT_VALUE, false, null, expireTime);
        this.value = value;
    }

    protected VaultValueAttachment(@NotNull UUID id, @NotNull String name, double value, boolean received, @Nullable LocalDateTime expireTime) {
        super(id, name, Type.VAULT_VALUE, received, null, expireTime);
        this.value = value;
    }

    public double value() {
        return value;
    }

    @Override
    protected @NotNull ItemType defaultPreviewType() {
        return ItemType.EMERALD;
    }

    @Override
    public void apply(@NotNull Player player) {
        // TODO: Implement this
        player.sendMessage("Received " + value + " from " + getName());
    }

    @Override
    public byte @NotNull [] serialize() {
        return ByteBuffer.allocate(8).putDouble(value).array();
    }

    public static @NotNull VaultValueAttachment deserialize(@NotNull UUID id, @NotNull String name, boolean received, byte @NotNull [] data, @Nullable LocalDateTime expireTime) {
        return new VaultValueAttachment(id, name, ByteBuffer.wrap(data).getDouble(), received, expireTime);
    }

    @Override
    public @NotNull VaultValueAttachment create(boolean received) {
        return new VaultValueAttachment(getName(), value, getExpireTime().orElse(null));
    }

}
