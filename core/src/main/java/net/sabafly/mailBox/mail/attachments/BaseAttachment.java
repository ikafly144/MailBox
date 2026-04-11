package net.sabafly.mailBox.mail.attachments;

import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.mail.IAttachment;
import net.sabafly.mailbox.api.mail.attachments.AttachmentContent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public abstract class BaseAttachment<T extends BaseAttachment<T, C>, C extends AttachmentContent<?>> implements IAttachment<T, C> {

    @NotNull
    private final UUID id;
    @NotNull
    private String name;
    @NotNull
    private final Type type;
    @NotNull
    private ItemStack previewItem;
    private boolean opened;
    @Nullable
    private LocalDateTime receivedTime;
    @Nullable
    private Duration expireDuration;

    protected BaseAttachment(@NotNull String name, @NotNull Type type, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration) {
        this(UUID.randomUUID(), name, type, opened, previewItem, receivedTime, expireDuration);
    }

    protected BaseAttachment(@NotNull BaseAttachment<?, C> base) {
        this(base.id, base.name, base.type, base.opened, base.previewItem, base.receivedTime, base.expireDuration);
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
        return expireTime().map(t -> t.isBefore(LocalDateTime.now())).orElse(false);
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof BaseAttachment<?, ?> that) {
            return id.equals(that.id);
        }
        return false;
    }

    protected static abstract class BaseBuilder<T extends BaseAttachment<T, C>, B extends BaseBuilder<T, B, C>, C extends AttachmentContent<?>> extends BaseAttachment<T, C> implements net.sabafly.mailbox.api.mail.attachments.Attachment.Builder<B, C> {

        protected final C content;

        protected BaseBuilder(@NotNull UUID id, @NotNull String name, @NotNull Type type, boolean opened, @NotNull ItemStack previewItem, @Nullable LocalDateTime receivedTime, @Nullable Duration expireDuration, C content) {
            super(id, name, type, opened, previewItem, receivedTime, expireDuration);
            this.content = content;
        }

        @Override
        public void apply(@NotNull Player player) {
            throw new UnsupportedOperationException("BaseBuilder does not support apply operation");
        }

        @Override
        public boolean checkRequirement(@NotNull Player player) {
            throw new UnsupportedOperationException("BaseBuilder does not support checkRequirement operation");
        }

        @Override
        public boolean consumeRequirement(@NotNull Player player) {
            throw new UnsupportedOperationException("BaseBuilder does not support consumeRequirement operation");
        }

        @Override
        public byte @NotNull [] serialize() {
            throw new UnsupportedOperationException("BaseBuilder does not support serialize operation");
        }

        @SuppressWarnings("unchecked")
        @Override
        public B name(@NotNull String name) {
            super.name = name;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public B icon(@NotNull ItemStack icon) {
            super.previewItem = icon;
            return (B) this;
        }

        @Override
        public @NonNull C content() {
            return content;
        }

        @Override
        public T build() {
            return create(false);
        }
    }

}
