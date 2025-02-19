package net.sabafly.mailBox.mail;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.sabafly.mailBox.MailBox.database;

public class Mail implements Comparable<Mail> {

    @NotNull
    @Getter
    private final UUID id;
    @Nullable
    private final UUID sender;
    @NotNull
    private final UUID receiver;
    @NotNull
    @Getter
    private final String title;
    @NotNull
    @Getter
    private final String content;
    @Getter
    private final @NotNull List<? extends @NotNull Attachment<?>> attachments;
    @Getter
    @Setter
    private boolean read;
    @Getter
    private final LocalDateTime sentTime;

    @NotNull
    public static Mail createNow(Player sender, OfflinePlayer receiver, String title, String content, List<Attachment<?>> attachments) {
        return new Mail(UUID.randomUUID(), sender.getUniqueId(), receiver.getUniqueId(), title, content, attachments, false, LocalDateTime.now());
    }

    public static Mail createNow(@Nullable MailUser sender, @NotNull MailUser receiver, String title, String content, List<? extends Attachment<?>> attachments) {
        return new Mail(UUID.randomUUID(), Optional.ofNullable(sender).map(MailUser::uuid).orElse(null), receiver.uuid(), title, content, attachments, false, LocalDateTime.now());
    }

    public Mail(
            @NotNull UUID id,
            @Nullable UUID sender,
            @NotNull UUID receiver,
            @NotNull String title,
            @NotNull String content,
            @NotNull List<? extends Attachment<?>> attachments,
            boolean read,
            @NotNull LocalDateTime sentTime) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.title = title;
        this.content = content;
        this.attachments = attachments;
        this.read = read;
        this.sentTime = sentTime;
    }

    @Override
    public int compareTo(@NotNull Mail o) {
        int result = sentTime.compareTo(o.sentTime);
        if (result == 0) {
            result = title.compareTo(o.title);
        }
        if (result == 0) {
            result = content.length() - o.content.length();
        }
        if (result == 0) {
            result = id.compareTo(o.id);
        }
        return result;
    }

    public @Nullable MailUser getSender() {
        return sender == null ? null : database().getUser(sender);
    }

    public @NotNull MailUser getReceiver() {
        return database().getUser(receiver);
    }

}
