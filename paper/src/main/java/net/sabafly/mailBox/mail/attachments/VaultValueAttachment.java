package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.utils.EconomyUtils;
import net.sabafly.mailbox.api.mail.attachments.VaultValueContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;
import static net.sabafly.mailBox.MailBox.config;
import static net.sabafly.mailBox.MailBox.messages;

@SuppressWarnings("UnstableApiUsage")
public class VaultValueAttachment extends BaseAttachment<VaultValueAttachment, VaultValueContent> {

    private final VaultValueContent value;

    private VaultValueAttachment(@Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, double value) {
        super(EconomyUtils.getEconomy().format(value), Type.VAULT_VALUE, false, ItemType.EMERALD.createItemStack(), receivedTime, expireDuration);
        this.value = VaultValueContent.of(value);
    }

    protected VaultValueAttachment(@NotNull UUID id, boolean received, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, double value) {
        super(id, EconomyUtils.getEconomy().format(value), Type.VAULT_VALUE, received, ItemType.EMERALD.createItemStack(), receivedTime, expireDuration);
        this.value = VaultValueContent.of(value);
    }

    public VaultValueAttachment(@NotNull BaseAttachment<?, VaultValueContent> base) {
        super(base);
        this.value = base.content();
    }

    @Override
    public void apply(@NotNull Player player) {
        EconomyUtils.getEconomy().depositPlayer(player, content().value());
        player.sendMessage(miniMessage().deserialize(messages().deposit
                .replaceAll("\\{value}", Matcher.quoteReplacement(EconomyUtils.getEconomy().format(content().value())))
        ));
    }

    @Override
    public boolean checkRequirement(@NotNull Player player) {
        return EconomyUtils.getEconomy().has(player, content().value());
    }

    @Override
    public boolean consumeRequirement(@NotNull Player player) {
        return EconomyUtils.getEconomy().withdrawPlayer(player, content().value()).transactionSuccess();
    }

    @Override
    public byte @NotNull [] serialize() {
        return ByteBuffer.allocate(8).putDouble(content().value()).array();
    }

    public static @NotNull VaultValueAttachment deserialize(@NotNull UUID id, @NotNull String ignoredName, boolean received, byte @NotNull [] data, @NotNull ItemStack ignoredPreviewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        return new VaultValueAttachment(id, received, receivedTime, expireDuration, ByteBuffer.wrap(data).getDouble());
    }

    @Override
    public @NotNull VaultValueAttachment create(boolean opened) {
        return new VaultValueAttachment(LocalDateTime.now(), Optional.ofNullable(expireDuration()).orElse(config().mail.getExpirationDuration()), content().value());
    }

    public static @NotNull VaultValueAttachment createNew(double value) {
        return new VaultValueAttachment(null, config().mail.getExpirationDuration(), value);
    }

    @Override
    public @NotNull Component format() {
        return format(content())
                .append(Component.text(": "))
                .append(getName());
    }

    private static Component format(VaultValueContent content) {
        return Component.text("VAULT_VALUE[")
                .append(Component.text(EconomyUtils.getEconomy().format(content.value())))
                .append(Component.text("]"));
    }

    @Override
    public @NonNull VaultValueContent content() {
        return value;
    }

    public static final class VaultValueAttachmentBuilder extends BaseBuilder<VaultValueAttachment, VaultValueAttachmentBuilder, VaultValueContent> {

        public static VaultValueAttachmentBuilder builder(double value) {
            return new VaultValueAttachmentBuilder(UUID.randomUUID(), EconomyUtils.getEconomy().format(value), false, ItemType.EMERALD.createItemStack(), null, null, value);
        }

        private VaultValueAttachmentBuilder(@NotNull UUID id, @NotNull String name, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, double value) {
            super(id, name, Type.VAULT_VALUE, opened, previewItem, receivedTime, expireDuration, VaultValueContent.of(value));
        }

        @Override
        public @NotNull VaultValueAttachment build() {
            return new VaultValueAttachment(LocalDateTime.now(), Optional.ofNullable(expireDuration()).orElse(config().mail.getExpirationDuration()), content().value());
        }

        @Override
        public @NonNull VaultValueAttachment create(boolean opened) {
            return new VaultValueAttachment(this);
        }

        @Override
        public @NotNull Component format() {
            return VaultValueAttachment.format(content())
                    .append(Component.text(": "))
                    .append(getName());
        }
    }

}
