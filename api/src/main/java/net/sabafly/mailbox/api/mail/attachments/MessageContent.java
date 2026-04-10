package net.sabafly.mailbox.api.mail.attachments;

import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

public class MessageContent extends SimpleContent<Component> implements AttachmentContent<Component> {

    public static MessageContent of(Component content) {
        return new MessageContent(content);
    }

    private MessageContent(Component content) {
        super(content);
    }

     @Override
     public @NonNull Component value() {
         return super.value();
     }
}
