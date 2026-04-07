package net.sabafly.mailBox.mail;

import lombok.Setter;
import net.sabafly.mailbox.api.mail.Template;
import net.sabafly.mailbox.api.mail.User;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static net.sabafly.mailBox.MailBox.config;

public class MailTemplate implements Comparable<MailTemplate>, Template {
    private final @NotNull UUID id;
    private @NotNull String subject;
    private @NotNull String content;
    private @NotNull List<@NotNull Attachment<?>> attachment;

    @Setter
    private boolean autoSend;
    @Setter
    private @NotNull User sender;
    @Setter
    private @Nullable LocalDateTime startTime;
    @Setter
    private @Nullable LocalDateTime endTime;
    @Setter
    private @Nullable Duration interval;
    @Setter
    private @Nullable String permission;

    public MailTemplate(MailTemplate template) {
        this(template.id, template.subject, template.content, template.attachment, template.autoSend, template.sender, template.startTime, template.endTime, template.interval, template.permission);
    }

    public MailTemplate(
            @NotNull UUID id,
            @NotNull String subject,
            @NotNull String content,
            @NotNull List<@NotNull Attachment<?>> attachment,
            boolean autoSend,
            @NotNull User sender,
            @Nullable LocalDateTime startTime,
            @Nullable LocalDateTime endTime,
            @Nullable Duration interval,
            @Nullable String permission) {
        this.id = id;
        this.subject = subject;
        this.content = content;
        this.attachment = new ArrayList<>(attachment);
        this.autoSend = autoSend;
        this.sender = sender;
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.permission = permission;
    }

    public static MailTemplate createNow(@NotNull User p, @NotNull String title, @NotNull String content, @NotNull List<@NotNull Attachment<?>> attachments) {
        return new MailTemplate(UUID.randomUUID(), title, content, attachments, false, p, null, null, null, null);
    }

    public @NotNull UUID id() {
        return id;
    }

    @Override
    public @NotNull String subject() {
        return subject;
    }

    public void setSubject(@NotNull String title) {
        this.subject = title;
    }

    public @NotNull String content() {
        return content;
    }

    public void setContent(@NotNull String content) {
        this.content = content;
    }

    public @NotNull List<@NotNull Attachment<?>> attachment() {
        return attachment;
    }

    public void attachment(@NotNull List<@NotNull Attachment<?>> attachment) {
        this.attachment = new ArrayList<>(attachment);
    }

    public boolean autoSend() {
        return autoSend;
    }

    public @NonNull User sender() {
        return sender;
    }

    public @Nullable LocalDateTime startTime() {
        return startTime;
    }

    public @Nullable LocalDateTime endTime() {
        return endTime;
    }

    public @Nullable Duration interval() {
        return interval;
    }

    @ApiStatus.Internal
    public @Nullable Long intervalSeconds() {
        return Optional.ofNullable(interval).map(Duration::getSeconds).orElse(null);
    }

    public @Nullable String permission() {
        return permission;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MailTemplate) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.subject, that.subject) &&
               Objects.equals(this.content, that.content) &&
               Objects.equals(this.attachment, that.attachment) &&
                this.autoSend == that.autoSend &&
               Objects.equals(this.sender, that.sender) &&
               Objects.equals(this.startTime, that.startTime) &&
               Objects.equals(this.endTime, that.endTime) &&
               Objects.equals(this.interval, that.interval) &&
               Objects.equals(this.permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subject, content, attachment, autoSend, sender, startTime, endTime, interval, permission);
    }

    @Override
    public String toString() {
        return "Template[" +
               "id=" + id + ", " +
               "setSubject=" + subject + ", " +
               "setContent=" + content + ", " +
               "attachment=" + attachment + ", " +
               "autoSend=" + autoSend + ", " +
               "sender=" + sender + ", " +
               "startTime=" + startTime + ", " +
               "endTime=" + endTime + ", " +
               "interval=" + interval + ", " +
               "permission=" + permission + ']';
    }

    public @NotNull Mail createMail(@NotNull User receiver) {
        List<Attachment<?>> newAttachments = attachment.stream().map(Attachment::create).collect(Collectors.toList());
        String title = this.subject
                .replaceAll("\\{player}", Matcher.quoteReplacement(receiver.name()))
                .replaceAll("\\{interval}", (intervalCount() + 1) + "")
                .replaceAll("\\{date}", Matcher.quoteReplacement(LocalDateTime.now().format(DateTimeFormatter.ofPattern(config().mail.dateFormat))));
        String content = this.content
                .replaceAll("\\{player}", Matcher.quoteReplacement(receiver.name()))
                .replaceAll("\\{interval}", (intervalCount() + 1) + "")
                .replaceAll("\\{date}", Matcher.quoteReplacement(LocalDateTime.now().format(DateTimeFormatter.ofPattern(config().mail.dateFormat))));
        return Mail.createFromTemplateNow(sender, receiver, title, content, newAttachments);
    }

    @Override
    public int compareTo(@NotNull MailTemplate o) {
        return id.compareTo(o.id);
    }

    public long intervalCount() {
        return Optional.ofNullable(this.interval()).map(d -> Duration.between(Objects.requireNonNull(this.startTime()), LocalDateTime.now()).dividedBy(d)).orElse(0L);
    }

    public static final class TemplateBuilder extends MailTemplate implements Template.Builder {

        public TemplateBuilder(@NotNull UUID id, @NotNull String title, @NotNull String content, @NotNull List<@NotNull Attachment<?>> attachment, boolean autoSend, @NotNull User sender, @Nullable LocalDateTime startTime, @Nullable LocalDateTime endTime, @Nullable Duration interval, @Nullable String permission) {
            super(id, title, content, attachment, autoSend, sender, startTime, endTime, interval, permission);
        }

        @Override
        public Builder subject(String subject) {
            super.subject = subject;
            return this;
        }

        @Override
        public Builder content(String content) {
            super.content = content;
            return this;
        }

        @Override
        public Builder sender(User sender) {
            super.sender = sender;
            return this;
        }

        @Override
        public Template build() {
            return new MailTemplate(this);
        }
    }

}
