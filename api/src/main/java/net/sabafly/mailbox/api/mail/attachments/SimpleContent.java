package net.sabafly.mailbox.api.mail.attachments;

import org.jetbrains.annotations.NotNull;

public abstract class SimpleContent<C> implements AttachmentContent<C> {

    protected final C value;

    protected SimpleContent(C value) {
        this.value = value;
    }

    @Override
    public @NotNull C value() {
        return value;
    }

}
