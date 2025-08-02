package net.sabafly.mailBox.mail;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static net.sabafly.mailBox.MailBox.config;

public final class MailTemplate implements Comparable<MailTemplate> {
    private final @NotNull UUID id;
    private final @NotNull String title;
    private final @NotNull String content;
    private final @NotNull List<@NotNull Attachment<?>> attachment;

    @Setter
    private boolean autoSend;
    @Setter
    private @Nullable MailUser sender;
    @Setter
    private @Nullable LocalDateTime startTime;
    @Setter
    private @Nullable LocalDateTime endTime;
    @Setter
    private @Nullable Duration interval;
    @Setter
    private @Nullable String permission;

    public MailTemplate(MailTemplate template) {
        this(template.id, template.title, template.content, template.attachment, template.autoSend, template.sender, template.startTime, template.endTime, template.interval, template.permission);
    }

    public MailTemplate(
            @NotNull UUID id,
            @NotNull String title,
            @NotNull String content,
            @NotNull List<@NotNull Attachment<?>> attachment,
            boolean autoSend,
            @Nullable MailUser sender,
            @Nullable LocalDateTime startTime,
            @Nullable LocalDateTime endTime,
            @Nullable Duration interval,
            @Nullable String permission) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.attachment = new ArrayList<>(attachment);
        this.autoSend = autoSend;
        this.sender = sender;
        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
        this.permission = permission;
    }

    public static MailTemplate createNow(@Nullable MailUser p, @NotNull String title, @NotNull String content, @NotNull List<@NotNull Attachment<?>> attachments) {
        return new MailTemplate(UUID.randomUUID(), title, content, attachments, false, p, null, null, null, null);
    }

    public @NotNull UUID id() {
        return id;
    }

    public @NotNull String title() {
        return title;
    }

    public @NotNull String content() {
        return content;
    }

    public @NotNull List<@NotNull Attachment<?>> attachment() {
        return attachment;
    }

    public void attachment(@NotNull List<@NotNull Attachment<?>> attachment) {
        this.attachment.clear();
        this.attachment.addAll(attachment);
    }

    public boolean autoSend() {
        return autoSend;
    }

    public @Nullable MailUser sender() {
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
                Objects.equals(this.title, that.title) &&
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
        return Objects.hash(id, title, content, attachment, autoSend, sender, startTime, endTime, interval, permission);
    }

    @Override
    public String toString() {
        return "MailTemplate[" +
                "id=" + id + ", " +
                "title=" + title + ", " +
                "content=" + content + ", " +
                "attachment=" + attachment + ", " +
                "autoSend=" + autoSend + ", " +
                "sender=" + sender + ", " +
                "startTime=" + startTime + ", " +
                "endTime=" + endTime + ", " +
                "interval=" + interval + ", " +
                "permission=" + permission + ']';
    }

    public @NotNull Mail createMail(@NotNull MailUser user) {
        List<Attachment<?>> newAttachments = attachment.stream().map(Attachment::create).collect(Collectors.toList());
        String title = this.title
                .replaceAll("\\{player}", Matcher.quoteReplacement(Optional.ofNullable(Bukkit.getOfflinePlayer(user.uuid()).getName()).orElse(user.uuid().toString())))
                .replaceAll("\\{interval}", (intervalCount() + 1) + "")
                .replaceAll("\\{date}", Matcher.quoteReplacement(LocalDateTime.now().format(DateTimeFormatter.ofPattern(config().mail.dateFormat))));
        String content = this.content
                .replaceAll("\\{player}", Matcher.quoteReplacement(Optional.ofNullable(Bukkit.getOfflinePlayer(user.uuid()).getName()).orElse(user.uuid().toString())))
                .replaceAll("\\{interval}", (intervalCount() + 1) + "")
                .replaceAll("\\{date}", Matcher.quoteReplacement(LocalDateTime.now().format(DateTimeFormatter.ofPattern(config().mail.dateFormat))));
        return Mail.createNow(sender, user, title, content, newAttachments);
    }

    @Override
    public int compareTo(@NotNull MailTemplate o) {
        return id.compareTo(o.id);
    }

    public long intervalCount() {
        return Optional.ofNullable(this.interval()).map(d -> Duration.between(Objects.requireNonNull(this.startTime()), LocalDateTime.now()).dividedBy(d)).orElse(0L);
    }
}
