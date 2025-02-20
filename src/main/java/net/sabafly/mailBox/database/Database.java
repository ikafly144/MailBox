package net.sabafly.mailBox.database;

import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.MailUser;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;

public interface Database {

    int PAGE_SIZE = 27;

    void setup();

    void reload();

    void close();

    @NotNull MailUser getUser(@NotNull UUID uuid);

    default @NotNull SortedSet<@NotNull Mail> getMails(@NotNull MailUser user, @NotNull TriState read, int page) {
        return getMails(user, read, PAGE_SIZE, (page - 1) * PAGE_SIZE);
    }

    // 全てのメールを取得
    default @NotNull SortedSet<@NotNull Mail> getAllMails(@NotNull MailUser user, @NotNull TriState read) {
        return getMails(user, read, Integer.MAX_VALUE, 0);
    }


    @NotNull SortedSet<@NotNull Mail> getMails(@NotNull MailUser user, @NotNull TriState read, int limit, int offset);

    int countMails(@NotNull MailUser user, @NotNull TriState read);

    @NotNull Optional<@NotNull Mail> getMail(@NotNull UUID id);

    void createMail(@NotNull Mail mail);

    void updateMail(@NotNull Mail mail);

    void deleteMail(@NotNull Mail mail);

    void createMailTemplate(@NotNull MailTemplate template);

    void updateMailTemplate(@NotNull MailTemplate template);

    Optional<@NotNull MailTemplate> getMailTemplate(@NotNull UUID id);

    @NotNull List<@NotNull MailTemplate> getMailTemplates(int page);

    @NotNull List<@NotNull MailTemplate> getAllMailTemplates();

    void deleteMailTemplate(@NotNull MailTemplate template);

    <T extends Attachment<T>> Optional<@NotNull Attachment<T>> getAttachment(@NotNull UUID id, @NotNull Class<T> clazz) throws IllegalArgumentException;

    Optional<@NotNull Attachment<?>> getAttachment(@NotNull UUID id);

    void createAttachment(@NotNull Attachment<?> attachment);

    void deleteAttachment(@NotNull Attachment<?> attachment);

    void updateAttachment(@NotNull Attachment<?> attachment);

    <T extends Attachment<T>> @NotNull List<@NotNull Attachment<T>> getAttachments(Class<T> type);

    void createUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval);

    void deleteUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval);

    boolean hasUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval);

    @NotNull Optional<@NotNull LocalDateTime> getUserTemplateTime(@NotNull MailUser user, @NotNull MailTemplate template, int interval);

    @NotNull List<@NotNull Pair<MailTemplate, Integer>> getUserTemplates(@NotNull MailUser user, int page);

    @NotNull List<@NotNull MailUser> getUserTemplatesByTemplate(@NotNull MailTemplate template, int interval, int page);

    void deleteAllUserNotification(@NotNull MailUser user);

    @NotNull List<@NotNull Mail> getAllUserNotification(@NotNull MailUser user);

}
