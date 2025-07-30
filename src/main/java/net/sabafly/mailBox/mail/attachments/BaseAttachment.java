package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.mail.Attachment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public abstract class BaseAttachment<T extends BaseAttachment<T>> implements Attachment<T> {

    @NotNull
    private final UUID id;
    @NotNull
    private final String name;
    @NotNull
    private final Type type;
    @NotNull
    private final ItemStack previewItem;
    private boolean opened;
    @Nullable
    private LocalDateTime receivedTime;
    @Nullable
    private Duration expireDuration;

    protected BaseAttachment(@NotNull String name, @NotNull Type type, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        this(UUID.randomUUID(), name, type, opened, previewItem, receivedTime, expireDuration);
    }

    protected BaseAttachment(@NotNull UUID id, @NotNull String name, @NotNull Type type, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.previewItem = previewItem;
        this.opened = opened;
        this.receivedTime = receivedTime;
        this.expireDuration = expireDuration;
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public @NotNull Component getName() {
        return miniMessage().deserialize(name);
    }

    @Override
    public @NotNull String getPlainName() {
        return name;
    }

    @Override
    public @NotNull Type getType() {
        return type;
    }

    @Override
    public boolean opened() {
        return opened;
    }

    @Override
    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    @Override
    public @Nullable LocalDateTime getReceivedTime() {
        return receivedTime;
    }

    @Override
    public void setReceivedTime(@NotNull LocalDateTime receivedTime) {
        if (this.receivedTime != null) {
            return; // Prevent overwriting if already set
        }
        this.receivedTime = receivedTime;
    }

    @Override
    public boolean isTemplate() {
        return receivedTime == null;
    }

    @Override
    public boolean isExpired() {
        return expireDuration != null && receivedTime != null && !isTemplate() && LocalDateTime.now().isAfter(receivedTime.plus(expireDuration));
    }

    @Override
    public @NotNull Optional<Duration> expireDuration() {
        return Optional.ofNullable(expireDuration);
    }

    @Override
    public void expireDuration(@Nullable Duration expireDuration) {
        this.expireDuration = expireDuration;
    }

    @Override
    public @NotNull ItemStack getPreviewItem() {
        return previewItem;
    }

    @Override
    public String toString() {
        return "BaseAttachment{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

}
