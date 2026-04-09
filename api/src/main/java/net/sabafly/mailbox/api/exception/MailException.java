package net.sabafly.mailbox.api.exception;

public class MailException extends Exception {

    public static final MailException USER_NOT_FOUND = new MailException("User not found.");
    public static final MailException NOT_FOUND = new MailException("Mail not found.");
    public static final MailException ALREADY_READ = new MailException("Mail has already been read.");
    public static final MailException ATTACHMENT_NOT_FOUND = new MailException("Attachment not found.");
    public static final MailException ATTACHMENT_ALREADY_OPENED = new MailException("Attachment has already been opened.");
    public static final MailException ALREADY_EXIST_USER = new MailException("User already exists.");
    public static final MailException TEMPLATE_NOT_FOUND = new MailException("Template not found.");

    public MailException(String message) {
        super(message);
    }
}
