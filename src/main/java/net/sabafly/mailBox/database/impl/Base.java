package net.sabafly.mailBox.database.impl;

import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.database.Database;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.MailUser;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@SuppressWarnings({"FieldCanBeLocal", "CallToPrintStackTrace"})
public abstract class Base implements Database {

    private QueryRunner runner;

    private final String CREATE_TABLE_USERS = """
            CREATE TABLE IF NOT EXISTS mailbox_users (
                uuid VARCHAR(36) PRIMARY KEY
            )
            """;

    private final String CREATE_TABLE_MAILS = """
            CREATE TABLE IF NOT EXISTS mailbox_mails (
                id VARCHAR(36) PRIMARY KEY,
                sender VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE SET NULL,
                receiver VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE CASCADE,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                is_read BOOLEAN NOT NULL,
                sentTime TIMESTAMP NOT NULL
            )
            """;

    private final String CREATE_TABLE_MAIL_ATTACHMENTS = """
            CREATE TABLE IF NOT EXISTS mailbox_mail_attachments (
                id VARCHAR(36),
                mail_id VARCHAR(36) REFERENCES mailbox_mails(id) ON DELETE CASCADE,
                type VARCHAR(255) NOT NULL,
                name TEXT NOT NULL,
                received BOOLEAN NOT NULL,
                preview_item LONGBLOB DEFAULT NULL,
                data LONGBLOB NOT NULL,
                receive_time TIMESTAMP,
                expire_duration BIGINT,
                PRIMARY KEY (id)
            )
            """;

    private final String CREATE_TABLE_TEMPLATES = """
            CREATE TABLE IF NOT EXISTS mailbox_templates (
                id VARCHAR(36) PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                auto_send BOOLEAN NOT NULL,
                sender VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE SET NULL,
                start_time TIMESTAMP,
                end_time TIMESTAMP,
                send_interval BIGINT,
                permission TEXT
            )
            """;

    private final String CREATE_TABLE_TEMPLATE_ATTACHMENTS = """
            CREATE TABLE IF NOT EXISTS mailbox_template_attachments (
                id VARCHAR(36),
                template_id VARCHAR(36) REFERENCES mailbox_templates(id) ON DELETE CASCADE,
                type VARCHAR(255) NOT NULL,
                name TEXT NOT NULL,
                received BOOLEAN NOT NULL,
                preview_item LONGBLOB DEFAULT NULL,
                data LONGBLOB NOT NULL,
                expire_duration BIGINT,
                PRIMARY KEY (id)
            )
            """;

    private final String CREATE_TABLE_USER_TEMPLATE = """
            CREATE TABLE IF NOT EXISTS mailbox_user_templates (
                user_id VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE CASCADE,
                template_id VARCHAR(36) REFERENCES mailbox_templates(id) ON DELETE CASCADE,
                interval_count INT,
                received_time TIMESTAMP NOT NULL,
                PRIMARY KEY (user_id, template_id, interval_count)
            )
            """;

    private final String CREATE_TABLE_USER_NOTIFICATION = """
            CREATE TABLE IF NOT EXISTS mailbox_user_notifications (
                user_id VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE CASCADE,
                mail_id VARCHAR(36) REFERENCES mailbox_mails(id) ON DELETE CASCADE,
                sent_time TIMESTAMP NOT NULL
            )
            """;

    public abstract Connection getConnection();

    @Override
    public void setup() {
        runner = new QueryRunner();

        try (Connection conn = getConnection()) {
            runner.execute(conn, CREATE_TABLE_USERS);
            runner.execute(conn, CREATE_TABLE_MAILS);
            runner.execute(conn, CREATE_TABLE_MAIL_ATTACHMENTS);
            runner.execute(conn, CREATE_TABLE_TEMPLATES);
            runner.execute(conn, CREATE_TABLE_TEMPLATE_ATTACHMENTS);
            runner.execute(conn, CREATE_TABLE_USER_TEMPLATE);
            runner.execute(conn, CREATE_TABLE_USER_NOTIFICATION);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        reload();
    }

    @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
    @Override
    public void reload() {
        try (Connection conn = getConnection()) {
            var fallbackPreview = ItemStack.of(Material.STONE).serializeAsBytes();
            runner.execute(conn, """
                    ALTER TABLE mailbox_mail_attachments ADD COLUMN IF NOT EXISTS preview_item LONGBLOB DEFAULT NULL
                    """);
            runner.execute(conn, """
                    ALTER TABLE mailbox_mail_attachments DROP COLUMN IF EXISTS item_type
                    """);
            runner.execute(conn, """
                    UPDATE mailbox_mail_attachments SET preview_item = ? WHERE preview_item IS NULL
                    """, fallbackPreview);
            runner.execute(conn, """
                    ALTER TABLE mailbox_template_attachments ADD COLUMN IF NOT EXISTS preview_item LONGBLOB DEFAULT NULL
                    """);
            runner.execute(conn, """
                    ALTER TABLE mailbox_template_attachments DROP COLUMN IF EXISTS item_type
                    """);
            runner.execute(conn, """
                    UPDATE mailbox_template_attachments SET preview_item = ? WHERE preview_item IS NULL
                    """, fallbackPreview);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isUserExists(@NotNull UUID uuid) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, "SELECT COUNT(*) FROM mailbox_users WHERE uuid = ?", rs -> {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }, uuid.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to check if user exists");
    }

    @Override
    public @NotNull MailUser getUser(@NotNull UUID uuid) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT * FROM mailbox_users WHERE uuid = ?
                    """, rs -> {
                if (rs.next()) {
                    return new MailUser(UUID.fromString(rs.getString("uuid")));
                }
                MailUser user = new MailUser(uuid);
                runner.execute(conn, "INSERT INTO mailbox_users (uuid) VALUES (?)", uuid.toString());
                return user;
            }, uuid.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get user");
    }

    @Override
    public @NotNull List<@NotNull MailUser> getAllUsers() {
        try (Connection conn = getConnection()) {
            return runner.query(conn, "SELECT * FROM mailbox_users", rs -> {
                List<MailUser> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(new MailUser(UUID.fromString(rs.getString("uuid"))));
                }
                return users;
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get all users");
    }

    @Override
    public @NotNull SortedSet<Mail> getMails(@NotNull MailUser user, @NotNull TriState read, int limit, int offset) {
        try (Connection conn = getConnection()) {
            if (read != TriState.NOT_SET) {
                return runner.query(conn, """
                        SELECT * FROM mailbox_mails WHERE receiver = ? AND is_read = ? ORDER BY sentTime DESC LIMIT ? OFFSET ?
                        """, rs -> {
                    SortedSet<Mail> mails = new TreeSet<>();
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString("id"));
                        UUID sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).orElse(null);
                        UUID receiver = UUID.fromString(rs.getString("receiver"));
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        boolean isRead = rs.getBoolean("is_read");
                        LocalDateTime sentTime = rs.getTimestamp("sentTime").toLocalDateTime();
                        Mail mail = new Mail(id, sender, receiver, title, content, List.of(), isRead, sentTime);
                        mail.attachments(getMailAttachments(mail));
                        mails.add(mail);
                    }
                    return mails;
                }, user.uuid().toString(), read.toBoolean(), limit, offset);
            } else {
                return runner.query(conn, """
                        SELECT * FROM mailbox_mails WHERE receiver = ? ORDER BY sentTime DESC LIMIT ? OFFSET ?
                        """, rs -> {
                    SortedSet<Mail> mails = new TreeSet<>();
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString("id"));
                        UUID sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).orElse(null);
                        UUID receiver = UUID.fromString(rs.getString("receiver"));
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        boolean isRead = rs.getBoolean("is_read");
                        LocalDateTime sentTime = rs.getTimestamp("sentTime").toLocalDateTime();
                        Mail mail = new Mail(id, sender, receiver, title, content, List.of(), isRead, sentTime);
                        mail.attachments(getMailAttachments(mail));
                        mails.add(mail);
                    }
                    return mails;
                }, user.uuid().toString(), limit, offset);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mails");
    }

    @Override
    public int countMails(@NotNull MailUser user, @NotNull TriState read) {
        try (Connection conn = getConnection()) {
            if (read != TriState.NOT_SET) {
                return runner.query(conn, "SELECT COUNT(*) FROM mailbox_mails WHERE receiver = ? AND is_read = ?", rs -> {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }, user.uuid().toString(), read.toBoolean());
            } else {
                return runner.query(conn, "SELECT COUNT(*) FROM mailbox_mails WHERE receiver = ?", rs -> {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                    return 0;
                }, user.uuid().toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to count mails");
    }

    @Override
    public @NotNull Optional<@NotNull Mail> getMail(@NotNull UUID id) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, """
                    SELECT * FROM mailbox_mails WHERE id = ?
                    """, rs -> {
                if (rs.next()) {
                    UUID sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).orElse(null);
                    UUID receiver = UUID.fromString(rs.getString("receiver"));
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    boolean read = rs.getBoolean("is_read");
                    LocalDateTime sentTime = rs.getTimestamp("sentTime").toLocalDateTime();
                    Mail mail = new Mail(id, sender, receiver, title, content, List.of(), read, sentTime);
                    mail.attachments(getMailAttachments(mail));
                    return mail;
                }
                return null;
            }, id.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail");
    }

    @Override
    public void createMail(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    INSERT INTO mailbox_mails (id, sender, receiver, title, content, is_read, sentTime) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, mail.getId().toString(), mail.getSenderId(), mail.getReceiver().uuid().toString(), mail.getTitle(), mail.getContent(), mail.isRead(), mail.getSentTime());
            mail.attachments().forEach(attachment -> createMailAttachment(mail, attachment));
            createUserNotification(mail.getReceiver(), mail);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateMail(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    UPDATE mailbox_mails SET sender = ?, receiver = ?, title = ?, content = ?, is_read = ?, sentTime = ? WHERE id = ?
                    """, mail.getSenderId(), mail.getReceiver().uuid().toString(), mail.getTitle(), mail.getContent(), mail.isRead(), mail.getSentTime(), mail.getId().toString());
            mail.attachments().forEach(attachment -> updateMailAttachment(mail, attachment));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteMail(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            deleteAllMailAttachments(mail);
            runner.execute(conn, """
                    DELETE FROM mailbox_mails WHERE id = ?
                    """, mail.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createMailTemplate(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    INSERT INTO mailbox_templates (id, title, content, auto_send, sender, start_time, end_time, send_interval, permission) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, template.id().toString(), template.title(), template.content(), template.autoSend(), template.sender() == null ? null : template.sender().uuid().toString(), template.startTime(), template.endTime(), template.intervalSeconds(), template.permission());
            template.attachment().forEach(attachment -> createTemplateAttachment(template, attachment));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateMailTemplate(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            var old = getTemplateAttachments(template);
            runner.execute(conn, """
                    UPDATE mailbox_templates SET title = ?, content = ?, auto_send = ?, sender = ?, start_time = ?, end_time = ?, send_interval = ?, permission = ? WHERE id = ?
                    """, template.title(), template.content(), template.autoSend(), template.sender() == null ? null : template.sender().uuid().toString(), template.startTime(), template.endTime(), template.intervalSeconds(), template.permission(), template.id().toString());
            template.attachment().forEach(attachment -> updateTemplateAttachment(template, attachment));
            // Delete attachments that are no longer in the template
            old.stream().filter(a -> !template.attachment().contains(a)).forEach(a -> deleteTemplateAttachment(template, a));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<@NotNull MailTemplate> getMailTemplate(@NotNull UUID id) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, """
                    SELECT * FROM mailbox_templates WHERE id = ?
                    """, rs -> {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    boolean autoSend = rs.getBoolean("auto_send");
                    MailUser sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).map(this::getUser).orElse(null);
                    Date startTime = rs.getTimestamp("start_time");
                    Date endTime = rs.getTimestamp("end_time");
                    Duration interval = Optional.of(rs.getLong("send_interval")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    String permission = rs.getString("permission");
                    MailTemplate template = new MailTemplate(id, title, content, List.of(), autoSend, sender, LocalDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault()), LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault()), interval, permission);
                    template.attachment(getTemplateAttachments(template));
                    return template;
                }
                return null;
            }, id.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail template");
    }

    @Override
    public @NotNull List<@NotNull MailTemplate> getMailTemplates(int page) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT * FROM mailbox_templates ORDER BY id LIMIT ? OFFSET ?
                    """, rs -> {
                List<MailTemplate> templates = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    boolean autoSend = rs.getBoolean("auto_send");
                    @Nullable MailUser sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).map(this::getUser).orElse(null);
                    @Nullable LocalDateTime startTime = Optional.ofNullable(rs.getTimestamp("start_time")).map(timestamp -> LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())).orElse(null);
                    @Nullable LocalDateTime endTime = Optional.ofNullable(rs.getTimestamp("end_time")).map(timestamp -> LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())).orElse(null);
                    @Nullable Duration interval = Optional.of(rs.getLong("send_interval")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    @Nullable String permission = rs.getString("permission");
                    MailTemplate template = new MailTemplate(id, title, content, List.of(), autoSend, sender, startTime, endTime, interval, permission);
                    template.attachment(getTemplateAttachments(template));
                    templates.add(template);
                }
                return templates;
            }, PAGE_SIZE, (page - 1) * PAGE_SIZE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail templates");
    }

    @Override
    public @NotNull List<@NotNull MailTemplate> getAllMailTemplates() {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT * FROM mailbox_templates
                    """, rs -> {
                List<MailTemplate> templates = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    boolean autoSend = rs.getBoolean("auto_send");
                    @Nullable MailUser sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).map(this::getUser).orElse(null);
                    @Nullable LocalDateTime startTime = rs.getTimestamp("start_time") == null ? null : LocalDateTime.ofInstant(rs.getTimestamp("start_time").toInstant(), ZoneId.systemDefault());
                    @Nullable LocalDateTime endTime = rs.getTimestamp("end_time") == null ? null : LocalDateTime.ofInstant(rs.getTimestamp("end_time").toInstant(), ZoneId.systemDefault());
                    @Nullable Duration interval = Optional.of(rs.getLong("send_interval")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    @Nullable String permission = rs.getString("permission");
                    MailTemplate template = new MailTemplate(id, title, content, List.of(), autoSend, sender, startTime, endTime, interval, permission);
                    template.attachment(getTemplateAttachments(template));
                    templates.add(template);
                }
                return templates;
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail templates");
    }

    @Override
    public void deleteMailTemplate(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            deleteAllTemplateAttachments(template);
            runner.execute(conn, """
                    DELETE FROM mailbox_templates WHERE id = ?
                    """, template.id().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createMailAttachment(@NotNull Mail mail, @NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    INSERT INTO mailbox_mail_attachments (id, mail_id, type, name, received, preview_item, data, receive_time, expire_duration) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, attachment.getId().toString(), mail.getId().toString(), attachment.getType().name(), attachment.getPlainName(), attachment.opened(), Optional.of(attachment.getPreviewItem()).map(ItemStack::serializeAsBytes).orElseThrow(), attachment.serialize(), Optional.ofNullable(attachment.getReceivedTime()).map(Timestamp::valueOf).orElse(null), attachment.expireDuration().map(Duration::getSeconds).orElse(0L));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteMailAttachment(@NotNull Mail mail, @NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, "DELETE FROM mailbox_mail_attachments WHERE id = ?", attachment.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllMailAttachments(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, "DELETE FROM mailbox_mail_attachments WHERE mail_id = ?", mail.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateMailAttachment(@NotNull Mail mail, @NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, "UPDATE mailbox_mail_attachments SET type = ?, name = ?, received = ?, preview_item = ?, data = ?, EXPIRE_DURATION = ? WHERE id = ?", attachment.getType().name(), attachment.getPlainName(), attachment.opened(), Optional.of(attachment.getPreviewItem()).map(ItemStack::serializeAsBytes).orElseThrow(), attachment.serialize(), attachment.expireDuration().map(Duration::getSeconds).orElse(0L), attachment.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<@NotNull Attachment<?>> getMailAttachments(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, "SELECT * FROM mailbox_mail_attachments WHERE mail_id = ?", rs -> {
                List<Attachment<?>> attachments = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    Attachment.Type type = Attachment.Type.valueOf(rs.getString("type"));
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    ItemStack previewItem = ItemStack.deserializeBytes(rs.getBytes("preview_item"));
                    byte[] data = rs.getBytes("data");
                    LocalDateTime receivedTime = Optional.ofNullable(rs.getTimestamp("receive_time")).map(timestamp -> LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())).orElse(null);
                    Duration expireDuration = Optional.of(rs.getLong("expire_duration")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    attachments.add(Attachment.deserialize(type, id, name, received, data, previewItem, receivedTime, expireDuration));
                }
                return attachments;
            }, mail.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail attachments");
    }

    @Override
    public @NotNull Optional<@NotNull Attachment<?>> getMailAttachment(@NotNull UUID id) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, "SELECT * FROM mailbox_mail_attachments WHERE id = ?", rs -> {
                if (rs.next()) {
                    Attachment.Type type = Attachment.Type.valueOf(rs.getString("type"));
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    ItemStack previewItem = ItemStack.deserializeBytes(rs.getBytes("preview_item"));
                    byte[] data = rs.getBytes("data");
                    LocalDateTime receivedTime = Optional.ofNullable(rs.getTimestamp("receive_time")).map(timestamp -> LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())).orElse(null);
                    Duration expireDuration = Optional.of(rs.getLong("expire_duration")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    return Attachment.deserialize(type, id, name, received, data, previewItem, receivedTime, expireDuration);
                }
                return null;
            }, id.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail attachment");
    }

    @Override
    public void createTemplateAttachment(@NotNull MailTemplate template, @NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, "INSERT INTO mailbox_template_attachments (id, template_id, type, name, received, preview_item, data, EXPIRE_DURATION) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", attachment.getId().toString(), template.id().toString(), attachment.getType().name(), attachment.getPlainName(), attachment.opened(), Optional.of(attachment.getPreviewItem()).map(ItemStack::serializeAsBytes).orElseThrow(), attachment.serialize(), attachment.expireDuration().orElse(null));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteTemplateAttachment(@NotNull MailTemplate template, @NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, "DELETE FROM mailbox_template_attachments WHERE id = ?", attachment.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllTemplateAttachments(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, "DELETE FROM mailbox_template_attachments WHERE template_id = ?", template.id().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateTemplateAttachment(@NotNull MailTemplate template, @NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            int row = runner.execute(conn, "UPDATE mailbox_template_attachments SET type = ?, name = ?, received = ?, preview_item = ?, data = ?, EXPIRE_DURATION = ? WHERE id = ?", attachment.getType().name(), attachment.getPlainName(), attachment.opened(), Optional.of(attachment.getPreviewItem()).map(ItemStack::serializeAsBytes).orElseThrow(), attachment.serialize(), attachment.expireDuration().map(Duration::getSeconds).orElse(0L), attachment.getId().toString());
            if (row == 0) {
                createTemplateAttachment(template, attachment);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<@NotNull Attachment<?>> getTemplateAttachments(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, "SELECT * FROM mailbox_template_attachments WHERE template_id = ?", rs -> {
                List<Attachment<?>> attachments = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    Attachment.Type type = Attachment.Type.valueOf(rs.getString("type"));
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    ItemStack previewItem = ItemStack.deserializeBytes(rs.getBytes("preview_item"));
                    byte[] data = rs.getBytes("data");
                    Duration expireDuration = Optional.of(rs.getLong("expire_duration")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    attachments.add(Attachment.deserialize(type, id, name, received, data, previewItem, null, expireDuration));
                }
                return attachments;
            }, template.id().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get template attachments");
    }

    @Override
    public @NotNull Optional<@NotNull Attachment<?>> getTemplateAttachment(@NotNull UUID id) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, "SELECT * FROM mailbox_template_attachments WHERE id = ?", rs -> {
                if (rs.next()) {
                    Attachment.Type type = Attachment.Type.valueOf(rs.getString("type"));
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    ItemStack previewItem = ItemStack.deserializeBytes(rs.getBytes("preview_item"));
                    byte[] data = rs.getBytes("data");
                    Duration expireDuration = Optional.of(rs.getLong("expire_duration")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    return Attachment.deserialize(type, id, name, received, data, previewItem, null, expireDuration);
                }
                return null;
            }, id.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get template attachment");
    }

    @Override
    public boolean hasUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT * FROM mailbox_user_templates WHERE user_id = ? AND template_id = ? AND interval_count = ?
                    """, ResultSet::next, user.uuid().toString(), template.id().toString(), interval);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to check user template");
    }

    @Override
    public @NotNull Optional<@NotNull LocalDateTime> getUserTemplateTime(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, """
                    SELECT received_time FROM mailbox_user_templates WHERE user_id = ? AND template_id = ? AND interval_count = ?
                    """, rs -> {
                if (rs.next()) {
                    Date receivedTime = rs.getTimestamp("received_time");
                    return LocalDateTime.ofInstant(receivedTime.toInstant(), ZoneId.systemDefault());
                }
                return null;
            }, user.uuid().toString(), template.id().toString(), interval));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get user template time");
    }

    @Override
    public @NotNull List<@NotNull Pair<MailTemplate, Integer>> getUserTemplates(@NotNull MailUser user, int page) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT * FROM mailbox_user_templates WHERE user_id = ? ORDER BY received_time DESC LIMIT ? OFFSET ?
                    """, rs -> {
                List<Pair<MailTemplate, Integer>> templates = new ArrayList<>();
                while (rs.next()) {
                    UUID templateId = UUID.fromString(rs.getString("template_id"));
                    MailTemplate template = getMailTemplate(templateId).orElseThrow();
                    int interval = rs.getInt("interval_count");
                    templates.add(Pair.of(template, interval));
                }
                return templates;
            }, user.uuid().toString(), PAGE_SIZE, (page - 1) * PAGE_SIZE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get user templates");
    }

    @Override
    public @NotNull List<@NotNull MailUser> getUserTemplatesByTemplate(@NotNull MailTemplate template, int interval, int page) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT user_id FROM mailbox_user_templates WHERE template_id = ? AND interval_count = ? ORDER BY received_time DESC LIMIT ? OFFSET ?
                    """, rs -> {
                List<MailUser> users = new ArrayList<>();
                while (rs.next()) {
                    UUID userId = UUID.fromString(rs.getString("user_id"));
                    users.add(getUser(userId));
                }
                return users;
            }, template.id().toString(), interval, PAGE_SIZE, (page - 1) * PAGE_SIZE);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get user templates by template");
    }

    @Override
    public void createUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    INSERT INTO mailbox_user_templates (user_id, template_id, interval_count, received_time) VALUES (?, ?, ?, ?)
                    """, user.uuid().toString(), template.id().toString(), interval, Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    DELETE FROM mailbox_user_templates WHERE user_id = ? AND template_id = ? AND interval_count = ?
                    """, user.uuid().toString(), template.id().toString(), interval);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void createUserNotification(@NotNull MailUser user, @NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    INSERT INTO mailbox_user_notifications (user_id, mail_id, sent_time) VALUES (?, ?, ?)
                    """, user.uuid().toString(), mail.getId().toString(), Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllUserNotification(@NotNull MailUser user) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, """
                    DELETE FROM mailbox_user_notifications WHERE user_id = ?
                    """, user.uuid().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<@NotNull Mail> getAllUserNotification(@NotNull MailUser user) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, """
                    SELECT mail_id FROM mailbox_user_notifications WHERE user_id = ? ORDER BY sent_time DESC LIMIT ? OFFSET ?
                    """, rs -> {
                List<Mail> mails = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("mail_id"));
                    mails.add(getMail(id).orElseThrow());
                }
                return mails;
            }, user.uuid().toString(), Integer.MAX_VALUE, 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get user notifications");
    }
}
