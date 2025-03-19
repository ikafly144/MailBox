package net.sabafly.mailBox.mail.attachments;

import net.sabafly.mailBox.utils.EconomyUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

@SuppressWarnings("UnstableApiUsage")
public class VaultValueAttachment extends BaseAttachment<VaultValueAttachment> {

    private final double value;

    private VaultValueAttachment(@Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, double value) {
        super(EconomyUtils.getEconomy().format(value), Type.VAULT_VALUE, false, ItemType.EMERALD, receivedTime, expireDuration);
        this.value = value;
    }

    protected VaultValueAttachment(@NotNull UUID id, boolean received, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, double value) {
        super(id, EconomyUtils.getEconomy().format(value), Type.VAULT_VALUE, received, ItemType.EMERALD, receivedTime, expireDuration);
        this.value = value;
    }

    public double value() {
        return value;
    }

    @Override
    protected @NotNull ItemType defaultPreviewType() {
        return ItemType.PAPER;
    }

    @Override
    public void apply(@NotNull Player player) {
        EconomyUtils.getEconomy().depositPlayer(player, value);
        player.sendMessage(miniMessage().deserialize(config().messages.deposit
                .replace("{value}", EconomyUtils.getEconomy().format(value))
        ));
    }

    @Override
    public void cancel(@NotNull Player player) {
        EconomyUtils.getEconomy().depositPlayer(player, value);
        player.sendMessage(miniMessage().deserialize(config().messages.deposit
                .replace("{value}", EconomyUtils.getEconomy().format(value))
        ));
    }

    @Override
    public byte @NotNull [] serialize() {
        return ByteBuffer.allocate(8).putDouble(value).array();
    }

    public static @NotNull VaultValueAttachment deserialize(@NotNull UUID id, @NotNull String ignoredName, boolean received, byte @NotNull [] data, @Nullable ItemType ignoredItemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new VaultValueAttachment(id, received, receivedTime, expireDuration, ByteBuffer.wrap(data).getDouble());
    }

    @Override
    public @NotNull VaultValueAttachment create(boolean received) {
        return new VaultValueAttachment(received ? LocalDateTime.now() : null, config().mail.getExpirationTime(), value);
    }

    public static @NotNull Optional<VaultValueAttachment> createNew(double value, @NotNull Player player, @Nullable Duration expireTime) {
        if (!EconomyUtils.getEconomy().withdrawPlayer(player, value).transactionSuccess()) {
            return Optional.empty();
        }
        return Optional.of(new VaultValueAttachment(null, expireTime, value));
    }

}
