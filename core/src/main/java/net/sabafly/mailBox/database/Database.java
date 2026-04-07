package net.sabafly.mailBox.database;

import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailbox.api.mail.User;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull <U extends User> U registerUser(@NotNull U user);

    @Nullable <U extends User> U getUser(@Nullable UUID uuid);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isUserExists(@NotNull UUID uuid);

    @NotNull List<@NotNull User> getAllUsers();

    default @NotNull SortedSet<@NotNull Mail> getMails(@NotNull User user, @NotNull TriState read, int page) {
        return getMails(user, read, PAGE_SIZE, (page - 1) * PAGE_SIZE);
    }

    // 全てのメールを取得
    default @NotNull SortedSet<@NotNull Mail> getAllMails(@NotNull User user, @NotNull TriState read) {
        return getMails(user, read, Integer.MAX_VALUE, 0);
    }


    @NotNull SortedSet<@NotNull Mail> getMails(@NotNull User user, @NotNull TriState read, int limit, int offset);

    int countMails(@NotNull User user, @NotNull TriState read);

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

    void createMailAttachment(@NotNull Mail mail, @NotNull Attachment<?> attachment);

    void deleteMailAttachment(@NotNull Mail mail, @NotNull Attachment<?> attachment);

    void deleteAllMailAttachments(@NotNull Mail mail);

    void updateMailAttachment(@NotNull Mail mail, @NotNull Attachment<?> attachment);

    @NotNull List<@NotNull Attachment<?>> getMailAttachments(@NotNull Mail mail);

    @NotNull Optional<@NotNull Attachment<?>> getMailAttachment(@NotNull UUID id);

    void createTemplateAttachment(@NotNull MailTemplate template, @NotNull Attachment<?> attachment);

    void deleteTemplateAttachment(@NotNull MailTemplate template, @NotNull Attachment<?> attachment);

    void deleteAllTemplateAttachments(@NotNull MailTemplate template);

    void updateTemplateAttachment(@NotNull MailTemplate template, @NotNull Attachment<?> attachment);

    @NotNull List<@NotNull Attachment<?>> getTemplateAttachments(@NotNull MailTemplate template);

    @NotNull Optional<@NotNull Attachment<?>> getTemplateAttachment(@NotNull UUID id);

    void createUserTemplate(@NotNull User user, @NotNull MailTemplate template, int interval);

    void deleteUserTemplate(@NotNull User user, @NotNull MailTemplate template, int interval);

    boolean hasUserTemplate(@NotNull User user, @NotNull MailTemplate template, int interval);

    @NotNull Optional<@NotNull LocalDateTime> getUserTemplateTime(@NotNull User user, @NotNull MailTemplate template, int interval);

    @NotNull List<@NotNull Pair<MailTemplate, Integer>> getUserTemplates(@NotNull User user, int page);

    @NotNull List<@NotNull User> getUserTemplatesByTemplate(@NotNull MailTemplate template, int interval, int page);

    void deleteAllUserNotification(@NotNull User user);

    @NotNull List<@NotNull Mail> getAllUserNotification(@NotNull User user);

}
