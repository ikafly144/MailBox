package net.sabafly.mailbox.api.mail.attachments;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public final class ItemContent extends SimpleContent<ItemStack> implements AttachmentContent<ItemStack> {

    public static ItemContent of(ItemStack content) {
        return new ItemContent(content);
    }

    private ItemContent(ItemStack content) {
        super(content);
    }

    @Override
    public @NonNull ItemStack value() {
        return super.value().clone();
    }
}
