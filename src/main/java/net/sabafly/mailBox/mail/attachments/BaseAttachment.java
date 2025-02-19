package net.sabafly.mailBox.mail.attachments;

import net.sabafly.mailBox.mail.Attachment;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public abstract class BaseAttachment<T extends BaseAttachment<T>> implements Attachment<T> {

    @NotNull
    private final UUID id;
    @NotNull
    private final String name;
    @NotNull
    private final Type type;
    @Nullable
    private final ItemType itemType;
    private boolean received;
    @Nullable
    private final LocalDateTime expireTime;

    abstract protected @NotNull ItemType defaultPreviewType();

    protected BaseAttachment(@NotNull String name, @NotNull Type type, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        this(UUID.randomUUID(), name, type, received, itemType, expireTime);
    }

    protected BaseAttachment(@NotNull UUID id, @NotNull String name, @NotNull Type type, boolean received, @Nullable ItemType itemType, @Nullable LocalDateTime expireTime) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.itemType = itemType;
        this.received = received;
        this.expireTime = expireTime;
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull Type getType() {
        return type;
    }

    @Override
    public boolean received() {
        return received;
    }

    @Override
    public void setReceived(boolean received) {
        this.received = received;
    }

    @Override
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    @Override
    public @NotNull Optional<LocalDateTime> getExpireTime() {
        return Optional.ofNullable(expireTime);
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
