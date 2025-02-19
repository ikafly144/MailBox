package net.sabafly.mailBox.menu;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.sabafly.mailBox.utils.ThreadUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class OpenBookMenu extends BaseMenu {

    private Book book;
    private ItemStack bookItem;

    public OpenBookMenu(Player player, Book book) {
        super(player, 9, Component.empty());
        this.book = book;
    }

    public OpenBookMenu(Player player, ItemStack book) {
        super(player, 9, Component.empty());
        this.bookItem = book;
    }

    @Override
    public void open() {
        ThreadUtils.runSync(() -> {
            if (book != null) {
                player.openBook(book);
            } else if (bookItem != null) {
                player.openBook(bookItem);
            }
        });
    }

    @Override
    void setItems(@NotNull ClickRegistry clickRegistry) {
    }
}
