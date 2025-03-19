package net.sabafly.mailBox.configuration;

import io.papermc.paper.configuration.type.Duration;
import io.papermc.paper.configuration.type.DurationOrDisabled;
import net.sabafly.mailBox.database.Database;
import net.sabafly.mailBox.database.impl.H2;
import net.sabafly.mailBox.database.impl.MySQL;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Optional;

@ConfigSerializable
public class Config extends BaseConfig {

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
        public int zoneOffset = 0;
        public String dateFormat = "yyyy-MM-dd HH:mm:ss";
        public int maxMailCount = 100;
        @Range(from = 1, to = 27)
        public int maxAttachmentCount = 27;
        public DurationOrDisabled expirationTime = new DurationOrDisabled(Optional.of(Duration.of("7d")));

        public @Nullable java.time.Duration getExpirationTime() {
            return expirationTime.value().map(Duration::seconds).map(java.time.Duration::ofSeconds).orElse(null);
        }
    }

    public Messages messages = new Messages();

    @ConfigSerializable
    public static class Messages extends BaseConfig {
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
        public String mailTemplateLore = """
                <gray>Sender: <white>{sender}</white>
                <gray>Start: <white>{start}</white>
                <gray>End: <white>{end}</white>
                <gray>Interval: <white>{interval}</white>
                <gray>Attachments: <white>{attachments}</white>
                <gray>Auto Send: <white>{auto_send}</white>""";
        public String mailMenuMailLore = """
                <gray>From: <white>{sender}</white>
                <gray>Time: <white>{time}</white>
                <gray>Attachments: <white>{attachments}</white>
                <gray>Read: {read}</gray>""";
        public String attachmentLore = """
                <gray>Received: <white>{received}</white>
                <gray>Expires: <white>{expires}</white>""";
        public String read = "<green>Read</green>";
        public String unread = "<red>Unread</red>";
        public String content = "Content";
        public String contentValue = "Content: <bold><title></bold>";
        public String titleValue = "Title: <bold><title></bold>";
        public String setTitle = "Set Title";
        public String setContent = "Set Content";
        public String attachmentAppendItem = "<gray>Append Item</gray>";
        public String attachmentAppendCommand = "<gray>Append Command</gray>";
        public String setAttachment = "Set Attachment";
        public String send = "Send";
        public String createMailSuccess = "<green>Mail created successfully</green>";
        public String createMailError = "<red>Mail creation failed</red>";
        public String senderValue = "Sender: <bold><sender></bold>";
        public String setSender = "Set Sender";
        public String nextPage = "Next Page";
        public String previousPage = "Previous Page";
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
        public String createMailTemplateError = "<red>Mail template creation failed</red>";
        public String newMail = "<green>You have <count> new mail</green>";
        public String unreadMail = "<green>You have <count> unread mail</green>";
        public String unreceivedAttachment = "<yellow>You have <count> unreceived attachment</yellow>";
        public String received = "<green>Received</green>";
        public String notReceived = "<red>Not Received</red>";
        public String expired = "<red>Expired</red>";
        public String expiresNever = "<gray>Never</gray>";

        public String rightClickTo = "<gray>Right Click to {action}</gray>";
        public String leftClickTo = "<gray>Left Click to {action}</gray>";
        public String shiftClickTo = "<gray>Shift Click to {action}</gray>";
        public String clickActionDelete = "<red>Delete</red>";
        public String clickActionRead = "<green>Read</green>";
        public String clickActionUnread = "<red>Unread</red>";
        public String clickActionSend = "<green>Send</green>";
        public String clickActionCreate = "<green>Create</green>";
        public String clickActionOpen = "<green>Open</green>";
        public String clickActionReceive = "<green>Receive</green>";
        public String clickActionSet = "<green>Set</green>";
        public String clickActionUnset = "<red>Unset</red>";
        public String clickActionSetExpiration = "<green>Set Expiration</green>";

        public String unreceived = "Unreceived";
        public String page = "page";
        public String mails = "mails";
        public String noValue = "<gray>No Value</gray>";
        public String mailBoxFull = "<red>Receiver's mail box is full</red>";
        public String deposit = "Deposit {value}";
        public String setName = "Set Name";
        public String nameValue = "<gray>Name: <bold><yellow><name></yellow></bold></gray>";
        public String commandValue = "<gray>Command: <bold><yellow><command></yellow></bold></gray>";
        public String setCommand = "Set Command";
        public String append = "Append";
        public String attachmentAppendVault = "Append {currency}";
        public String displayItem = "Display Item";
        public String setExpiration = "Set Expiration";
        public String expirationValue = "<gray>Expiration: <bold><yellow><expiration></yellow></bold></gray>";
    }

}
