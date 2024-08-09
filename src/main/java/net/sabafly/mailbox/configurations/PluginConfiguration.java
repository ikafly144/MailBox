package net.sabafly.mailbox.configurations;

import net.sabafly.mailbox.type.MailAction;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@ConfigSerializable
public class PluginConfiguration extends ConfigurationPart {

    private static PluginConfiguration instance;
    public static final int CURRENT_VERSION = 3;

    public static @NotNull PluginConfiguration get() {
        return instance;
    }

    static void set(@NotNull PluginConfiguration configuration) {
        instance = configuration;
    }

    public static void save(@NotNull PluginConfiguration configuration) {
        set(configuration);
    }

    public PluginConfiguration() {
        super();
    }

    public PluginConfiguration(List<MailSchedule> mailSchedules) {
        super();
        this.mailSchedules = mailSchedules;
    }

    @Setting(ConfigurationPart.VERSION_FIELD)
    public int version = CURRENT_VERSION;

    public Messages messages = new Messages();

    @ConfigSerializable
    public static class Messages extends ConfigurationPart {
        public String mailBox = "MailBox";
        public String reload = "Configuration reloaded";
        public String receivedTime = "Received";
        public String sentMails ="Sent mail [name: <name>] [description: <description>] to <targets>";
        public String scheduledMail = "Scheduled mail [name: <name>] [description: <description>] at <time>";
        public String changeNextPage = "Next page";
        public String changePreviousPage = "Previous page";
        public String receivedMail = "<yellow>You have received a mail!";
        public String openMailBox = "<click:run_command:/mailbox>Click here or type /mailbox to open your mailbox";
        public String clearedMailBox = "Cleared your mailbox";
        public String clearMailBoxSuccess = "Successfully cleared mailbox";
    }

    public List<MailSchedule> mailSchedules = new ArrayList<>();

    @ConfigSerializable
    public static class MailSchedule extends ConfigurationPart {

        public MailSchedule() {
            super();
        }

        public MailSchedule(String name, String description, LocalDateTime time, String item, MailAction action) {
            this(name, description, time, item, action, LocalDateTime.now());
        }

        public MailSchedule(String name, String description, LocalDateTime time, String item, MailAction action, LocalDateTime start) {
            super();
            this.name = name;
            this.description = description;
            this.start = start;
            this.end = time;
            this.item = item;
            this.action = action;
        }

        public boolean enabled = true;
        public String name;
        public String description;
        public LocalDateTime start = LocalDateTime.now();
        public LocalDateTime end = null;
        public String item;
        public MailAction action = MailAction.NONE;
    }

}
