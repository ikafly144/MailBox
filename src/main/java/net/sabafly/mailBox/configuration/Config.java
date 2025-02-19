package net.sabafly.mailBox.configuration;

import io.papermc.paper.configuration.type.Duration;
import io.papermc.paper.configuration.type.DurationOrDisabled;
import net.sabafly.mailBox.database.Database;
import net.sabafly.mailBox.database.impl.H2;
import net.sabafly.mailBox.database.impl.MySQL;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Optional;

@ConfigSerializable
public class Config extends BaseConfig {

    public String prefix = "<red>[<white>MailBox</white>]</red>";

    public DatabaseConfig database = new DatabaseConfig();

    @ConfigSerializable
    public static class DatabaseConfig extends BaseConfig {

        public Type type = Type.H2;

        public enum Type {
            MYSQL,
            H2
        }

        public String host = "localhost";
        public int port = 3306;
        public String database = "mailBox";
        public String username = "root";
        public String password = "password";

        public Database loadDatabase() {
            return switch (type) {
                case MYSQL -> new MySQL();
                case H2 -> new H2();
            };
        }

    }

    public MailConfig mail = new MailConfig();

    @ConfigSerializable
    public static class MailConfig extends BaseConfig {
        public int maxMailCount = 100;
        public int maxAttachmentCount = 20;
        public DurationOrDisabled expirationTime = new DurationOrDisabled(Optional.of(Duration.of("7d")));
    }

    public Messages messages = new Messages();

    @ConfigSerializable
    public static class Messages extends BaseConfig {
        public String mailMenuMailLore = """
                <gray>From: <white>{sender}</white>
                <gray>Time: <white>{time}</white>
                <gray>Attachments: <white>{attachments}</white>
                <gray>Read: {read}</gray>""";
        @Comment("This field cannot use minimessage")
        public String systemName = "System";

        public String mailMenuTitle = "<red>Mail</red><white>Box</white>";
        public String mailViewerMenuTitle = "<red>Mail</red><white>Viewer</white>";
        public String createMailMenuTitle = "<red>Create</red><white>Mail</white>";
        public String inputMenuTitle = "<red>Input</red><white>Menu</white>";
        public String bookMenuTitle = "<red>Book</red><white>Menu</white>";
        public String attachmentMenuTitle = "<red>Attachment</red><white>Menu</white>";
        public String mailTemplateMenuTitle = "<red>Mail</red><white>Template</white>";
        public String mailTemplateEditMenuTitle = "<red>Mail</red><white>Template</white>";
        public String read = "<green>Read</green>";
        public String unread = "<red>Unread</red>";
        public String content = "Content";
        public String contentBook = "Content Book: <bold><title></bold>";
        public String title = "Title: <bold><title></bold>";
        public String setTitle = "Set Title";
        public String setContent = "Set Content";
        public String attachmentAppendItem = "<gray>Append Item</gray>";
        public String attachmentAppendCommand = "<gray>Append Command</gray>";
        public String setAttachment = "Set Attachment";
        public String send = "Send";
        public String createMailSuccess = "<green>Mail created successfully</green>";
        public String createMailError = "<red>Mail creation failed</red>";
        public String sender = "Sender: <bold><sender></bold>";
        public String setSender = "Set Sender";
        public String nextPage = "Next Page";
        public String previousPage = "Previous Page";
        public String mailTemplateLore = """
                <gray>Sender: <white>{sender}</white>
                <gray>Start: <white>{start}</white>
                <gray>End: <white>{end}</white>
                <gray>Interval: <white>{interval}</white>
                <gray>Attachments: <white>{attachments}</white>
                <gray>Auto Send: <white>{auto_send}</white>""";
        public String autoSend = "Auto Send";
        public String enabled = "<green>Enabled</green>";
        public String disabled = "<red>Disabled</red>";
        public String startTime = "Start Time";
        public String setStartTime = "Set Start Time";
        public String endTime = "End Time";
        public String setEndTime = "Set End Time";
        public String interval = "Interval";
        public String setInterval = "Set Interval";
        public String permissions = "Permissions";
        public String setPermissions = "Set Permissions";
        public String delete = "Delete";
        public String attachments = "Attachments";
        public String createMailTemplate = "Create Mail Template";
        public String createMailTemplateSuccess = "<green>Mail template created successfully</green>";
        public String newMail = "<green>You have <count> new mail</green>";
        public String unreadMail = "<green>You have <count> unread mail</green>";
    }

}
