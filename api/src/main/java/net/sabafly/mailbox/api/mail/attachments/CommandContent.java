package net.sabafly.mailbox.api.mail.attachments;

public class CommandContent extends SimpleContent<String> implements AttachmentContent<String> {

    public static CommandContent of(String content) {
        return new CommandContent(content);
    }

    private CommandContent(String content) {
        super(content);
    }

}
