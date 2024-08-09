package net.sabafly.mailbox.utils;

import net.sabafly.mailbox.MailBox;

import java.util.logging.Logger;

public final class LogUtil {

    private LogUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static Logger getLogger() {
        return MailBox.getPlugin(MailBox.class).getLogger();
    }

}
