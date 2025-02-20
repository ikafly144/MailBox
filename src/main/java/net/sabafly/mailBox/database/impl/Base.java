package net.sabafly.mailBox.database.impl;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.util.TriState;
import net.sabafly.mailBox.database.Database;
import net.sabafly.mailBox.mail.Attachment;
import net.sabafly.mailBox.mail.Mail;
import net.sabafly.mailBox.mail.MailTemplate;
import net.sabafly.mailBox.mail.MailUser;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@SuppressWarnings({"FieldCanBeLocal", "UnstableApiUsage", "CallToPrintStackTrace"})
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
                receiver VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE SET NULL,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                attachments TEXT,
                is_read BOOLEAN NOT NULL,
                sentTime TIMESTAMP NOT NULL
            )
            """;

    private final String CREATE_TABLE_TEMPLATES = """
            CREATE TABLE IF NOT EXISTS mailbox_templates (
                id VARCHAR(36) PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                attachments TEXT,
                auto_send BOOLEAN NOT NULL,
                sender VARCHAR(36) REFERENCES mailbox_users(uuid) ON DELETE SET NULL,
                start_time TIMESTAMP,
                end_time TIMESTAMP,
                send_interval BIGINT,
                permission TEXT
            )
            """;

    private final String CREATE_TABLE_ATTACHMENTS = """
            CREATE TABLE IF NOT EXISTS mailbox_attachments (
                id VARCHAR(36) PRIMARY KEY,
                type VARCHAR(255) NOT NULL,
                name TEXT NOT NULL,
                received BOOLEAN NOT NULL,
                item_type TEXT,
                expire_time TIMESTAMP,
                data LONGBLOB NOT NULL
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

    private final String SELECT_USER = """
            SELECT * FROM mailbox_users WHERE uuid = ?
            """;

    private final String SELECT_MAIL = """
            SELECT * FROM mailbox_mails WHERE id = ?
            """;

    private final String SELECT_MAILS = """
            SELECT * FROM mailbox_mails WHERE receiver = ? ORDER BY sentTime DESC LIMIT ? OFFSET ?
            """;

    private final String SELECT_MAILS_READ = """
            SELECT * FROM mailbox_mails WHERE receiver = ? AND is_read = ? ORDER BY sentTime DESC LIMIT ? OFFSET ?
            """;

    private final String INSERT_MAIL = """
            INSERT INTO mailbox_mails (id, sender, receiver, title, content, attachments, is_read, sentTime)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final String UPDATE_MAIL = """
            UPDATE mailbox_mails SET sender = ?, receiver = ?, title = ?, content = ?, attachments = ?, is_read = ?, sentTime = ? WHERE id = ?
            """;

    private final String DELETE_MAIL = """
            DELETE FROM mailbox_mails WHERE id = ?
            """;

    private final String SELECT_TEMPLATE = """
            SELECT * FROM mailbox_templates WHERE id = ?
            """;

    private final String SELECT_TEMPLATES = """
            SELECT * FROM mailbox_templates ORDER BY start_time DESC LIMIT ? OFFSET ?
            """;

    private final String INSERT_TEMPLATE = """
            INSERT INTO mailbox_templates (id, title, content, attachments, auto_send, sender, start_time, end_time, send_interval, permission)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final String UPDATE_TEMPLATE = """
            UPDATE mailbox_templates SET title = ?, content = ?, attachments = ?, auto_send = ?, sender = ?, start_time = ?, end_time = ?, send_interval = ?, permission = ? WHERE id = ?
            """;

    private final String DELETE_TEMPLATE = """
            DELETE FROM mailbox_templates WHERE id = ?
            """;

    private final String SELECT_ATTACHMENT = """
            SELECT * FROM mailbox_attachments WHERE id = ?
            """;

    private final String INSERT_ATTACHMENT = """
            INSERT INTO mailbox_attachments (id, type, name, received, item_type, expire_time, data)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private final String DELETE_ATTACHMENT = """
            DELETE FROM mailbox_attachments WHERE id = ?
            """;

    private final String UPDATE_ATTACHMENT = """
            UPDATE mailbox_attachments SET type = ?, name = ?, received = ?, item_type = ?, expire_time = ?, data = ? WHERE id = ?
            """;

    private final String INSERT_USER_TEMPLATE = """
            INSERT INTO mailbox_user_templates (user_id, template_id, interval_count, received_time)
            VALUES (?, ?, ?, ?)
            """;

    private final String DELETE_USER_TEMPLATE = """
            DELETE FROM mailbox_user_templates WHERE user_id = ? AND template_id = ? AND interval_count = ?
            """;

    private final String SELECT_USER_TEMPLATE = """
            SELECT * FROM mailbox_user_templates WHERE user_id = ? AND template_id = ? AND interval_count = ?
            """;

    private final String SELECT_USER_TEMPLATES = """
            SELECT * FROM mailbox_user_templates WHERE user_id = ? ORDER BY received_time DESC LIMIT ? OFFSET ?
            """;

    private final String SELECT_USER_TEMPLATES_BY_TEMPLATE = """
            SELECT * FROM mailbox_user_templates WHERE template_id = ? AND interval_count = ? ORDER BY received_time DESC LIMIT ? OFFSET ?
            """;

    private final String SELECT_USER_NOTIFICATIONS = """
            SELECT * FROM mailbox_user_notifications WHERE user_id = ? ORDER BY sent_time DESC LIMIT ? OFFSET ?
            """;

    private final String INSERT_USER_NOTIFICATION = """
            INSERT INTO mailbox_user_notifications (user_id, mail_id, sent_time)
            VALUES (?, ?, ?)
            """;

    private final String DELETE_USER_NOTIFICATIONS = """
            DELETE FROM mailbox_user_notifications WHERE user_id = ?
            """;

    public abstract Connection getConnection();

    @Override
    public void setup() {
        runner = new QueryRunner();

        try (Connection conn = getConnection()) {
            runner.execute(conn, CREATE_TABLE_USERS);
            runner.execute(conn, CREATE_TABLE_MAILS);
            runner.execute(conn, CREATE_TABLE_TEMPLATES);
            runner.execute(conn, CREATE_TABLE_ATTACHMENTS);
            runner.execute(conn, CREATE_TABLE_USER_TEMPLATE);
            runner.execute(conn, CREATE_TABLE_USER_NOTIFICATION);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        reload();
    }

    @Override
    public void reload() {
        try (Connection conn = getConnection()) {
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull MailUser getUser(@NotNull UUID uuid) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, SELECT_USER, rs -> {
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
    public @NotNull SortedSet<Mail> getMails(@NotNull MailUser user, @NotNull TriState read, int limit, int offset) {
        try (Connection conn = getConnection()) {
            if (read != TriState.NOT_SET) {
                return runner.query(conn, SELECT_MAILS_READ, rs -> {
                    SortedSet<Mail> mails = new TreeSet<>();
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString("id"));
                        UUID sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).orElse(null);
                        UUID receiver = UUID.fromString(rs.getString("receiver"));
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        List<Attachment<?>> attachments = new ArrayList<>(Arrays.stream(rs.getString("attachments").split(",")).filter(s -> !s.isBlank()).map(UUID::fromString).map(attachmentId -> getAttachment(attachmentId).orElseThrow()).toList());
                        boolean isRead = rs.getBoolean("is_read");
                        Date sentTime = rs.getTimestamp("sentTime");
                        mails.add(new Mail(id, sender, receiver, title, content, attachments, isRead, LocalDateTime.ofInstant(sentTime.toInstant(), ZoneId.systemDefault())));
                    }
                    return mails;
                }, user.uuid().toString(), read.toBoolean(), limit, offset);
            } else {
                return runner.query(conn, SELECT_MAILS, rs -> {
                    SortedSet<Mail> mails = new TreeSet<>();
                    while (rs.next()) {
                        UUID id = UUID.fromString(rs.getString("id"));
                        UUID sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).orElse(null);
                        UUID receiver = UUID.fromString(rs.getString("receiver"));
                        String title = rs.getString("title");
                        String content = rs.getString("content");
                        List<Attachment<?>> attachments = new ArrayList<>(Arrays.stream(rs.getString("attachments").split(",")).filter(s -> !s.isBlank()).map(UUID::fromString).map(attachmentId -> getAttachment(attachmentId).orElseThrow()).toList());
                        boolean isRead = rs.getBoolean("is_read");
                        Date sentTime = rs.getTimestamp("sentTime");
                        mails.add(new Mail(id, sender, receiver, title, content, attachments, isRead, LocalDateTime.ofInstant(sentTime.toInstant(), ZoneId.systemDefault())));
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
            return Optional.ofNullable(runner.query(conn, SELECT_MAIL, rs -> {
                if (rs.next()) {
                    UUID sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).orElse(null);
                    UUID receiver = UUID.fromString(rs.getString("receiver"));
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    List<Attachment<?>> attachments = new ArrayList<>(Arrays.stream(rs.getString("attachments").split(",")).filter(s -> !s.isBlank()).map(UUID::fromString).map(attachmentId -> getAttachment(attachmentId).orElseThrow()).toList());
                    boolean isRead = rs.getBoolean("is_read");
                    Date sentTime = rs.getTimestamp("sentTime");
                    return new Mail(id, sender, receiver, title, content, attachments, isRead, LocalDateTime.ofInstant(sentTime.toInstant(), ZoneId.systemDefault()));
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
            List<UUID> attachmentIds = new ArrayList<>();
            for (Attachment<?> attachment : mail.getAttachments()) {
                createAttachment(attachment);
                attachmentIds.add(attachment.getId());
            }
            MailUser sender = mail.getSender();
            MailUser receiver = mail.getReceiver();
            String title = mail.getTitle();
            String content = mail.getContent();
            String attachments = String.join(",", attachmentIds.stream().map(UUID::toString).toList());
            boolean isRead = mail.isRead();
            LocalDateTime sentTime = mail.getSentTime();
            runner.execute(conn, INSERT_MAIL, mail.getId().toString(), sender == null ? null : sender.toString(), receiver.toString(), title, content, attachments, isRead, Date.from(sentTime.atZone(ZoneId.systemDefault()).toInstant()));
            createUserNotification(receiver, mail);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateMail(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            List<UUID> attachmentIds = new ArrayList<>();
            for (Attachment<?> attachment : mail.getAttachments()) {
                getAttachment(attachment.getId()).ifPresentOrElse(this::updateAttachment, () -> createAttachment(attachment));
                attachmentIds.add(attachment.getId());
            }
            MailUser sender = mail.getSender();
            MailUser receiver = mail.getReceiver();
            String title = mail.getTitle();
            String content = mail.getContent();
            String attachments = String.join(",", attachmentIds.stream().map(UUID::toString).toList());
            boolean isRead = mail.isRead();
            LocalDateTime sentTime = mail.getSentTime();
            runner.execute(conn, UPDATE_MAIL, sender == null ? null : sender.toString(), receiver.toString(), title, content, attachments, isRead, Date.from(sentTime.atZone(ZoneId.systemDefault()).toInstant()), mail.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteMail(@NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            mail.getAttachments().forEach(this::deleteAttachment);
            runner.execute(conn, DELETE_MAIL, mail.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createMailTemplate(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            List<UUID> attachmentIds = new ArrayList<>();
            for (Attachment<?> attachment : template.attachment()) {
                Attachment<?> a = attachment.create();
                createAttachment(a);
                attachmentIds.add(a.getId());
            }
            UUID sender = template.sender() == null ? null : template.sender().uuid();
            String title = template.title();
            String content = template.content();
            String attachments = String.join(",", attachmentIds.stream().map(UUID::toString).toList());
            boolean autoSend = template.autoSend();
            LocalDateTime startTime = template.startTime();
            LocalDateTime endTime = template.endTime();
            Duration interval = template.interval();
            String permission = template.permission();
            runner.execute(conn, INSERT_TEMPLATE, template.id().toString(), title, content, attachments, autoSend, sender == null ? null : sender.toString(), startTime == null ? null : Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()), endTime == null ? null : Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant()), interval != null ? interval.toSeconds() : null, permission);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateMailTemplate(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            List<UUID> attachmentIds = new ArrayList<>();
            for (Attachment<?> attachment : template.attachment()) {
                Attachment<?> a = attachment.create();
                getAttachment(a.getId()).ifPresentOrElse(this::updateAttachment, () -> createAttachment(a));
                attachmentIds.add(a.getId());
            }
            UUID sender = template.sender() == null ? null : template.sender().uuid();
            String title = template.title();
            String content = template.content();
            String attachments = String.join(",", attachmentIds.stream().map(UUID::toString).toList());
            boolean autoSend = template.autoSend();
            LocalDateTime startTime = template.startTime();
            LocalDateTime endTime = template.endTime();
            Duration interval = template.interval();
            String permission = template.permission();
            runner.execute(conn, UPDATE_TEMPLATE, title, content, attachments, autoSend, sender == null ? null : sender.toString(), startTime == null ? null : Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant()), endTime == null ? null : Date.from(endTime.atZone(ZoneId.systemDefault()).toInstant()), interval != null ? interval.toSeconds() : null, permission, template.id().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<@NotNull MailTemplate> getMailTemplate(@NotNull UUID id) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, SELECT_TEMPLATE, rs -> {
                if (rs.next()) {
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    List<Attachment<?>> attachments = new ArrayList<>();
                    for (String attachmentId : rs.getString("attachments").split(",")) {
                        if (attachmentId.isBlank()) continue;
                        attachments.add(getAttachment(UUID.fromString(attachmentId)).orElseThrow());
                    }
                    boolean autoSend = rs.getBoolean("auto_send");
                    UUID sender = UUID.fromString(rs.getString("sender"));
                    Date startTime = rs.getTimestamp("start_time");
                    Date endTime = rs.getTimestamp("end_time");
                    Duration interval = Optional.of(rs.getLong("send_interval")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    String permission = rs.getString("permission");
                    return new MailTemplate(id, title, content, attachments, autoSend, new MailUser(sender), LocalDateTime.ofInstant(startTime.toInstant(), ZoneId.systemDefault()), LocalDateTime.ofInstant(endTime.toInstant(), ZoneId.systemDefault()), interval, permission);
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
            return runner.query(conn, SELECT_TEMPLATES, rs -> {
                List<MailTemplate> templates = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    List<Attachment<?>> attachments = new ArrayList<>();
                    for (String attachmentId : rs.getString("attachments").split(",")) {
                        if (attachmentId.isBlank()) continue;
                        attachments.add(getAttachment(UUID.fromString(attachmentId)).orElseThrow());
                    }
                    boolean autoSend = rs.getBoolean("auto_send");
                    @Nullable MailUser sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).map(this::getUser).orElse(null);
                    @Nullable LocalDateTime startTime = Optional.ofNullable(rs.getTimestamp("start_time")).map(timestamp -> LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())).orElse(null);
                    @Nullable LocalDateTime endTime = Optional.ofNullable(rs.getTimestamp("end_time")).map(timestamp -> LocalDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault())).orElse(null);
                    @Nullable Duration interval = Optional.of(rs.getLong("send_interval")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    @Nullable String permission = rs.getString("permission");
                    templates.add(new MailTemplate(id, title, content, attachments, autoSend, sender, startTime, endTime, interval, permission));
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
            return runner.query(conn, SELECT_TEMPLATES, rs -> {
                List<MailTemplate> templates = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    List<Attachment<?>> attachments = new ArrayList<>();
                    for (String attachmentId : rs.getString("attachments").split(",")) {
                        if (attachmentId.isBlank()) continue;
                        attachments.add(getAttachment(UUID.fromString(attachmentId)).orElseThrow());
                    }
                    boolean autoSend = rs.getBoolean("auto_send");
                    @Nullable MailUser sender = Optional.ofNullable(rs.getString("sender")).map(UUID::fromString).map(this::getUser).orElse(null);
                    @Nullable LocalDateTime startTime = rs.getTimestamp("start_time") == null ? null : LocalDateTime.ofInstant(rs.getTimestamp("start_time").toInstant(), ZoneId.systemDefault());
                    @Nullable LocalDateTime endTime = rs.getTimestamp("end_time") == null ? null : LocalDateTime.ofInstant(rs.getTimestamp("end_time").toInstant(), ZoneId.systemDefault());
                    @Nullable Duration interval = Optional.of(rs.getLong("send_interval")).filter(l -> l > 0).map(Duration::ofSeconds).orElse(null);
                    @Nullable String permission = rs.getString("permission");
                    templates.add(new MailTemplate(id, title, content, attachments, autoSend, sender, startTime, endTime, interval, permission));
                }
                return templates;
            }, Integer.MAX_VALUE, 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get mail templates");
    }

    @Override
    public void deleteMailTemplate(@NotNull MailTemplate template) {
        try (Connection conn = getConnection()) {
            template.attachment().forEach(this::deleteAttachment);
            runner.execute(conn, DELETE_TEMPLATE, template.id().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Attachment<T>> Optional<@NotNull Attachment<T>> getAttachment(@NotNull UUID id, @NotNull Class<T> clazz) throws IllegalArgumentException {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, SELECT_ATTACHMENT, rs -> {
                if (rs.next()) {
                    String type = rs.getString("type");
                    byte[] data;
                    try {
                        data = rs.getBinaryStream("data").readAllBytes();
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read attachment data");
                    }
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    @SuppressWarnings("PatternValidation") @KeyPattern String itemType = rs.getString("item_type");
                    Date expireTime = rs.getTimestamp("expire_time");
                    return (T) Attachment.deserialize(Attachment.Type.valueOf(type), id, name, received, data, itemType == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(Key.key(itemType)), expireTime == null ? null : LocalDateTime.ofInstant(expireTime.toInstant(), ZoneId.systemDefault()));
                }
                return null;
            }, id.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get attachment");
    }

    @Override
    public Optional<@NotNull Attachment<?>> getAttachment(@NotNull UUID id) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, SELECT_ATTACHMENT, rs -> {
                if (rs.next()) {
                    String type = rs.getString("type");
                    byte[] data;
                    try {
                        data = rs.getBinaryStream("data").readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    @SuppressWarnings("PatternValidation") @KeyPattern String itemType = rs.getString("item_type");
                    Date expireTime = rs.getTimestamp("expire_time");
                    return Attachment.deserialize(Attachment.Type.valueOf(type), id, name, received, data, itemType == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(Key.key(itemType)), expireTime == null ? null : LocalDateTime.ofInstant(expireTime.toInstant(), ZoneId.systemDefault()));
                }
                return null;
            }, id.toString()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get attachment");
    }

    @Override
    public void createAttachment(@NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, INSERT_ATTACHMENT, attachment.getId().toString(), attachment.getType().name(), attachment.getName(), attachment.received(), attachment.getPreviewType() == null ? null : attachment.getPreviewType().key().asMinimalString(), attachment.getExpireTime().map(expireTime -> Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant())).orElse(null), attachment.serialize());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAttachment(@NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, DELETE_ATTACHMENT, attachment.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateAttachment(@NotNull Attachment<?> attachment) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, UPDATE_ATTACHMENT, attachment.getType().name(), attachment.getName(), attachment.received(), attachment.getPreviewType() == null ? null : attachment.getPreviewType().key().asMinimalString(), attachment.getExpireTime().map(expireTime -> Date.from(expireTime.atZone(ZoneId.systemDefault()).toInstant())).orElse(null), attachment.serialize(), attachment.getId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T extends Attachment<T>> @NotNull List<@NotNull Attachment<T>> getAttachments(Class<T> type) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, SELECT_ATTACHMENT, rs -> {
                List<Attachment<T>> attachments = new ArrayList<>();
                while (rs.next()) {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String name = rs.getString("name");
                    boolean received = rs.getBoolean("received");
                    @SuppressWarnings("PatternValidation") @KeyPattern String itemType = rs.getString("item_type");
                    Date expireTime = rs.getTimestamp("expire_time");
                    byte[] data = rs.getBytes("data");
                    Attachment<?> attachment = Attachment.deserialize(Attachment.Type.valueOf(rs.getString("type")), id, name, received, data, itemType == null ? null : RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(Key.key(itemType)), expireTime == null ? null : LocalDateTime.ofInstant(expireTime.toInstant(), ZoneId.systemDefault()));
                    if (type.isInstance(attachment)) {
                        attachments.add(type.cast(attachment));
                    }
                }
                return attachments;
            }, type.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to get attachments");
    }

    @Override
    public boolean hasUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, SELECT_USER_TEMPLATE, ResultSet::next, user.uuid().toString(), template.id().toString(), interval);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to check user template");
    }

    @Override
    public @NotNull Optional<@NotNull LocalDateTime> getUserTemplateTime(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            return Optional.ofNullable(runner.query(conn, SELECT_USER_TEMPLATE, rs -> {
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
            return runner.query(conn, SELECT_USER_TEMPLATES, rs -> {
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
            return runner.query(conn, SELECT_USER_TEMPLATES_BY_TEMPLATE, rs -> {
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
            runner.execute(conn, INSERT_USER_TEMPLATE, user.uuid().toString(), template.id().toString(), interval, Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteUserTemplate(@NotNull MailUser user, @NotNull MailTemplate template, int interval) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, DELETE_USER_TEMPLATE, user.uuid().toString(), template.id().toString(), interval);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void createUserNotification(@NotNull MailUser user, @NotNull Mail mail) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, INSERT_USER_NOTIFICATION, user.uuid().toString(), mail.getId().toString(), Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteAllUserNotification(@NotNull MailUser user) {
        try (Connection conn = getConnection()) {
            runner.execute(conn, DELETE_USER_NOTIFICATIONS, user.uuid().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public @NotNull List<@NotNull Mail> getAllUserNotification(@NotNull MailUser user) {
        try (Connection conn = getConnection()) {
            return runner.query(conn, SELECT_USER_NOTIFICATIONS, rs -> {
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
