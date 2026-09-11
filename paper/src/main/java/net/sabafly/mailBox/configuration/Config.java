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

}
