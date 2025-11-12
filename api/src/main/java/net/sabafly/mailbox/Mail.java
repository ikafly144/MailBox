package net.sabafly.mailbox;

import net.sabafly.mailbox.attachments.Attachment;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Mail {

    @NotNull
    String title();

    @NotNull
    String content();

    @NotNull List<@NotNull Attachment<?>> attachments();

}
