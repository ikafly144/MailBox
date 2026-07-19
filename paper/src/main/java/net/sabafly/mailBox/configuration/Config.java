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

    public String locale = "en";

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

    public boolean enableGameMenuShortcut = true;
    public boolean enableQuickAction = true;
    public boolean enableMailNotification = false;
    public String rightArrowItem = "arrow";
    public String leftArrowItem = "arrow";

    public MailConfig mail = new MailConfig();

    @ConfigSerializable
    public static class MailConfig extends BaseConfig {
        public int zoneOffset = 0;
        public String dateFormat = "yyyy-MM-dd HH:mm:ss";
        public int maxMailCount = 100;
        public int mailPrice = 0;
        public int attachmentPrice = 0;
        @Range(from = 1, to = 27)
        public int maxAttachmentCount = 27;
        public DurationOrDisabled expirationTime = new DurationOrDisabled(Optional.of(Duration.of("7d")));

        public @Nullable java.time.Duration getExpirationDuration() {
            return expirationTime.value().map(Duration::seconds).map(java.time.Duration::ofSeconds).orElse(null);
        }
    }

    public Messages messages = new Messages();

    @ConfigSerializable
    public static class Messages {
        @Comment("This field cannot use minimessage")
        public String systemName = "System";

        public String mailMenuTitle = "<red>Mail</red><white>Box</white>";
        public String mailViewerMenuTitle = "<red>Mail</red><white>Viewer</white>";
        public String createMailMenuTitle = "<red>Create</red><white>Mail</white>";
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
        public String contentInfo = "Content: <bold><length> letters</bold>";
        public String subjectValue = "Subject: <bold><subject></bold>";
        public String setSubject = "Set Subject";
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
        public String clickActionEdit = "<green>Edit</green>";
        public String clickActionReply = "<green>Reply</green>";
        public String shiftLeftClickTo = "<gray>Shift+Left Click to {action}</gray>";
        public String clickActionClaimAll = "<green>Claim All Attachments</green>";

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
        public String closeButton = "Close";
        public String attachmentCommandError = "<red>Error while setting command attachment, please check your command</red>";
        public String submitButton = "Submit";
        public String emptyInputError = "<red>Input cannot be empty</red>";
        public String cancelButton = "Cancel";
        public String refreshButton = "Refresh";
        public String sendMailRecipientInput = "Recipient";
        public String sendMailMenuTitle = "<red>Send</red><white>Mail</white>";
        public String sendMailButton = "Send Mail";
        public String invalidRecipientError = "<red>Invalid recipient name</red>";
        public String notRegisteredError = "<red>Recipient is not registered</red>";
        public String inboxButton = "Inbox";
        public String inboxTooltip = "Open your inbox to view and manage your mails.";
        public String sendMailTooltip = "Send a mail to another player.";
        public String nextButton = "Next";
        public String inboxMenuTitle = "<red>Inbox</red><white>Menu</white> page {page}/{total_pages}";
        public String reloadSuccess = "<green>Configuration reloaded successfully</green>";
        public String howToOpenMail = "<gray>Use <command> or press <yellow><key:key.quickActions></yellow> to open your mail box.</gray>";
        public String sendTemplateSuccess = "Sent <template> to <count> players.";
        public String notEnoughMoney = "<red>You don't have <missing_amount> <currency> to perform this action</red>";
        public String mailPriceInfo ="<gray>Mail Price: <yellow><price> <currency></yellow></gray>";
        public String attachmentPriceInfo = "<gray>Attachment Price: <yellow><price> <currency> × <count></yellow></gray>";
        public String totalPriceInfo = "<gray>Total Price: <yellow><price> <currency></yellow></gray>";
        public String inboxOwner = "<dark_gray>{owner}'s inbox</dark_gray>";
        public String templateAlreadyExists = "<red>A template with the name <template> already exists</red>";
        public String notEnoughAttachmentContent = "<red>You don't have some contents of attachments.</red>";
        public String templateAttachmentList = "Attachments of <template>:<br><attachments>";
        public String templateNoAttachments = "<template> has no attachments";
        public String invalidAttachmentIndex = "<red>Invalid attachment index: <index></red>";
        public String templateAttachmentAddSuccess = "Added attachment <attachment> to <template>";
        public String templateAttachmentDeleteSuccess = "Deleted attachment <attachment> from <template>";
        public String templateCreateSuccess = "Created template <template>";
        public String templateDeleteSuccess = "Deleted template <template>";
        public String templateEditSubjectSuccess = "Edited subject of template <template> to <new_subject>";
        public String templateEditContentSuccess = "Edited content of template <template>";
        public String templateEditSenderSuccess = "Edited sender of template <template> to <sender>";
        public String deleteMailConfirmTitle = "<red>Confirm</red><white> Deletion</white>";
        public String deleteMailConfirmContent = "<gray>Are you sure you want to delete mail <mail_title>?</gray>";
        public String attachmentCannotOpen = "<red>You cannot open attachment <attachment>, reason: <reason></red>";
        public String attachmentExpired = "<red>Expired</red>";
        public String attachmentAlreadyReceived = "<red>Already received</red>";
    }

}
