package net.sabafly.mailBox.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static net.sabafly.mailBox.MailBox.config;

public class DateUtils {

    public static String format(LocalDateTime time) {
        return time.atOffset(ZoneOffset.ofHours(config().mail.zoneOffset)).format(DateTimeFormatter.ofPattern(config().mail.dateFormat));
    }

    public static LocalDateTime parse(String time) {
        return LocalDateTime.parse(time, DateTimeFormatter.ofPattern(config().mail.dateFormat).withZone(ZoneOffset.ofHours(config().mail.zoneOffset)));
    }

}
