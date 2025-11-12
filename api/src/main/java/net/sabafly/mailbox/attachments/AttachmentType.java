package net.sabafly.mailbox.attachments;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface AttachmentType<T> {
    public static final AttachmentType<ItemStack> ITEM = create("item", ItemStack.class);

    private static <T> AttachmentType<T> create(@NotNull String name, @NotNull Class<T> type) {
        return null;
    }
}
