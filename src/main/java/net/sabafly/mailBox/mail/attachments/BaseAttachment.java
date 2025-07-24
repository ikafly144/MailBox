package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.mail.Attachment;
import org.bukkit.inventory.ItemType;
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
    @Nullable
    private final ItemType itemType;
    private boolean opened;
    @Nullable
    private final LocalDateTime receivedTime;
    @Nullable
    private Duration expireDuration;

    abstract protected @NotNull ItemType defaultPreviewType();

    protected BaseAttachment(@NotNull String name, @NotNull Type type, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        this(UUID.randomUUID(), name, type, received, itemType, receivedTime, expireDuration);
    }

    protected BaseAttachment(@NotNull UUID id, @NotNull String name, @NotNull Type type, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.itemType = itemType;
        this.opened = received;
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
    public Optional<LocalDateTime> getReceivedTime() {
        return Optional.ofNullable(receivedTime);
    }

    @Override
    public boolean isTemplate() {
        return receivedTime == null;
    }

    @Override
    public boolean isExpired() {
        return expireDuration != null && receivedTime != null &&
                !isTemplate() && LocalDateTime.now().isAfter(receivedTime.plus(expireDuration));
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
    public @Nullable ItemType getPreviewType() {
        return itemType == null ? defaultPreviewType() : itemType;
    }

    @Override
    public String toString() {
        return "BaseAttachment{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

}
