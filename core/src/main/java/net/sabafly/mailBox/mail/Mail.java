package net.sabafly.mailBox.mail;

import lombok.Getter;
import lombok.Setter;
import net.sabafly.mailBox.utils.PlaceholderUtils;
import net.sabafly.mailbox.api.mail.User;
import net.sabafly.mailbox.api.mail.attachments.MailAttachment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.sabafly.mailBox.MailBox.database;

public class Mail implements Comparable<Mail>, net.sabafly.mailbox.api.mail.Mail {

    @NotNull
    @Getter
    private final UUID id;
    @NotNull
    private final User sender;
    @NotNull
    private final User receiver;
    @NotNull
    @Getter
    private final String title;
    @NotNull
    @Getter
    private final String content;
    private final @NotNull List<Attachment<?>> attachments;
    @Getter
    @Setter
    private boolean read;
    @Getter
    private final LocalDateTime sentTime;

    @NotNull
    public static Mail createFromUserNow(@NotNull User sender, User receiver, String title, String content, List<Attachment<?>> attachments) {
        return new Mail(UUID.randomUUID(), sender, receiver, title, content, attachments, false, LocalDateTime.now());
    }

    // For system mails with any sender
    public static Mail createFromTemplateNow(@NotNull User sender, @NotNull User receiver, String title, String content, List<Attachment<?>> attachments) {
        return new Mail(
                UUID.randomUUID(),
                sender,
                receiver,
                applyPlaceholder(receiver, title),
                applyPlaceholder(receiver, content),
                attachments,
                false,
                LocalDateTime.now());
    }

    private static String applyPlaceholder(User user, String text) {
        return user instanceof PlayerMailUser(
                org.bukkit.OfflinePlayer offlinePlayer,
                _
        ) ? PlaceholderUtils.setPlaceholder(offlinePlayer, text) : text;
    }


    public Mail(
            @NotNull UUID id,
            @NotNull User sender,
            @NotNull User receiver,
            @NotNull String title,
            @NotNull String content,
            @NotNull List<Attachment<?>> attachments,
            boolean read,
            @NotNull LocalDateTime sentTime) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.title = title;
        this.content = content;
        this.attachments = new ArrayList<>(attachments);
        this.attachments.forEach(attachment -> attachment.setReceivedTime(sentTime));
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

    public @NotNull User getSender() {
        return database().getOrCreateUser(sender);
    }

    public @NotNull User getReceiver() {
        return database().getOrCreateUser(receiver);
    }

    public void attachments(@NotNull List<@NotNull Attachment<?>> mailAttachments) {
        attachments.clear();
        attachments.addAll(mailAttachments);
    }

    @Override
    public @NotNull String subject() {
        return title;
    }

    @Override
    public @NotNull String content() {
        return content;
    }

    public @NotNull List<@NotNull MailAttachment> attachments() {
        return List.copyOf(attachments);
    }

    @ApiStatus.Internal
    public @NotNull List<@NotNull Attachment<?>> getAttachmentsInternal() {
        return attachments;
    }

    @Override
    public @NotNull User sender() {
        return database().getOrCreateUser(sender);
    }

    @Override
    public @NotNull User receiver() {
        return database().getOrCreateUser(receiver);
    }

    @Nullable
    @ApiStatus.Internal
    public String getSenderId() {
        return sender.id().toString();
    }
}
