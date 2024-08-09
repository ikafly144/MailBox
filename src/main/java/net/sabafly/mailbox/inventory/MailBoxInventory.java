package net.sabafly.mailbox.inventory;

import net.kyori.adventure.text.Component;
import net.sabafly.mailbox.configurations.PluginConfiguration;
import net.sabafly.mailbox.type.MailBoxEntry;
import net.sabafly.mailbox.type.PDCListType;
import net.sabafly.mailbox.type.PDCMailBoxEntryType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MailBoxInventory extends AbstractInventory {

    private final List<MailBoxEntry> entries;
    private List<Page> pages;
    private int currentPage = 0;
    private final PersistentDataContainer container;

    public static MailBoxInventory create(@NotNull List<MailBoxEntry> entries, @NotNull PersistentDataContainer container) {
        return new MailBoxInventory(54, Component.text(PluginConfiguration.get().messages.mailBox), entries, container);
    }

    private MailBoxInventory(int size, Component name, List<MailBoxEntry> entries, PersistentDataContainer container) {
        super(size, name);
        this.entries = new ArrayList<>(entries);
        this.pages = Page.create(entries);
        this.container = container;
        this.update();
    }

    public void update() {
        this.getInventory().clear();
        // Sort by received time
        // nullを許容するため、nullの場合は最新の順に並べる
        this.entries.sort((a, b) -> {
            if (a.getReceived() == null && b.getReceived() == null) {
                return 0;
            } else if (a.getReceived() == null) {
                return 1;
            } else if (b.getReceived() == null) {
                return -1;
            }
            return b.getReceived().compareTo(a.getReceived());
        });
        // Rebuild Pages
        this.pages.clear();
        this.pages = Page.create(this.entries);

        final var glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final var meta = glass.getItemMeta();
        meta.setHideTooltip(true);
        glass.setItemMeta(meta);
        for (int i = 0; i < 9; i++) {
            this.getInventory().setItem(i, glass);
            this.getInventory().setItem(9 * 5 + i, glass);
        }
        final var prev = new ItemStack(Material.ARROW);
        final var prevMeta = prev.getItemMeta();
        prevMeta.displayName(Component.text(PluginConfiguration.get().messages.changePreviousPage));
        prev.setItemMeta(prevMeta);
        this.getInventory().setItem(9 * 5 + 3, prev);
        final var next = new ItemStack(Material.ARROW);
        final var nextMeta = next.getItemMeta();
        nextMeta.displayName(Component.text(PluginConfiguration.get().messages.changeNextPage));
        next.setItemMeta(nextMeta);
        this.getInventory().setItem(9 * 5 + 5, next);
        if (this.pages.isEmpty()) {
            return;
        }
        Page page = this.pages.get(this.currentPage);
        int i = 0;
        for (MailBoxEntry entry : page.entries) {
            this.getInventory().setItem(9 + i++, entry.getItemStack());
        }
    }

    public void onClick(int slot, Player player, ClickType click) {
        if (slot < 9)
            return;
        else if (slot >= 9 * 5) {
            if (slot == (9 * 5 + 3)) {
                if (this.currentPage > 0) {
                    this.currentPage--;
                    this.update();
                }
            } else if (slot == 9 * 5 + 5) {
                if (this.currentPage < this.pages.size() - 1) {
                    this.currentPage++;
                    this.update();
                }
            }
            return;
        }
        int index = slot - 9;
        if (index >= this.pages.get(this.currentPage).entries.size())
            return;
        MailBoxEntry entry = new ArrayList<>(this.pages.get(this.currentPage).entries).get(index);

        if (click.isRightClick() && entry.getAction().used) {
            pages.forEach(p -> p.entries.remove(entry));
        }
        if (click.isLeftClick()) {
            entry.getAction().doAction(player);
        }

        entries.clear();
        pages.forEach(p -> entries.addAll(p.entries));
        container.set(MailBoxEntry.KEY, new PDCListType<>(new PDCMailBoxEntryType()), entries);

        update();
    }

    private static class Page {
        private final Set<MailBoxEntry> entries;

        private static List<Page> create(@Nullable List<MailBoxEntry> entries) {
            if (entries == null || entries.isEmpty()) {
                return new ArrayList<>();
            }

            List<Page> pages = new ArrayList<>();
            Set<MailBoxEntry> pageEntries = null;
            for (MailBoxEntry entry : entries) {
                if (pageEntries == null) {
                    pageEntries = new HashSet<>();
                }
                pageEntries.add(entry);
                if (pageEntries.size() == 36) {
                    pages.add(new Page(pageEntries));
                    pageEntries = null;
                }
            }
            if (pageEntries != null) {
                pages.add(new Page(pageEntries));
            }
            return pages;
        }

        private Page(Set<MailBoxEntry> entries) {
            this.entries = entries;
        }

    }

}
