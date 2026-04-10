package net.sabafly.mailbox.api.mail.attachments;

import org.jetbrains.annotations.NotNull;

public class VaultValueContent extends SimpleContent<Double> implements AttachmentContent<Double> {

    public static VaultValueContent of(double content) {
        return new VaultValueContent(content);
    }

    private VaultValueContent(double content) {
        super(content);
    }

     @Override
     public @NotNull Double value() {
         return super.value();
     }
}
