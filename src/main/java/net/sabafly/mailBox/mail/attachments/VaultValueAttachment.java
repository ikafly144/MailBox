package net.sabafly.mailBox.mail.attachments;

import net.sabafly.mailBox.utils.EconomyUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;

@SuppressWarnings("UnstableApiUsage")
public class VaultValueAttachment extends BaseAttachment<VaultValueAttachment> {

    private final double value;

    private VaultValueAttachment(double value, @Nullable LocalDateTime expireTime) {
        super(EconomyUtils.getEconomy().format(value), Type.VAULT_VALUE, false, null, expireTime);
        this.value = value;
    }

    protected VaultValueAttachment(@NotNull UUID id, double value, boolean received, @Nullable LocalDateTime expireTime) {
        super(id, EconomyUtils.getEconomy().format(value), Type.VAULT_VALUE, received, null, expireTime);
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
                .replace("{name}", getName())
        ));
    }

    @Override
    public void cancel(@NotNull Player player) {
        EconomyUtils.getEconomy().depositPlayer(player, value);
        player.sendMessage(miniMessage().deserialize(config().messages.deposit
                .replace("{value}", EconomyUtils.getEconomy().format(value))
                .replace("{name}", getName())
        ));
    }

    @Override
    public byte @NotNull [] serialize() {
        return ByteBuffer.allocate(8).putDouble(value).array();
    }

    public static @NotNull VaultValueAttachment deserialize(@NotNull UUID id, @NotNull String ignoredName, boolean received, byte @NotNull [] data, @Nullable LocalDateTime expireTime) {
        return new VaultValueAttachment(id, ByteBuffer.wrap(data).getDouble(), received, expireTime);
    }

    @Override
    public @NotNull VaultValueAttachment create(boolean received) {
        return new VaultValueAttachment(value, getExpireTime().orElse(null));
    }

    public static @NotNull Optional<VaultValueAttachment> createNew(double value, @NotNull Player player, @Nullable LocalDateTime expireTime) {
        if (!EconomyUtils.getEconomy().withdrawPlayer(player, value).transactionSuccess()) {
            return Optional.empty();
        }
        return Optional.of(new VaultValueAttachment(value, expireTime));
    }

}
