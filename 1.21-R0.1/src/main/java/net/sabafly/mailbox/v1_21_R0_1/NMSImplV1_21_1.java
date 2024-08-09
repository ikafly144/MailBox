package net.sabafly.mailbox.v1_21_R0_1;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.sabafly.mailbox.library.ICommandSourceStack;
import net.sabafly.mailbox.library.LibNMS;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextColor.color;

public class NMSImplV1_21_1 implements LibNMS {
    @Override
    public org.bukkit.inventory.ItemStack parseItemStack(String item) throws CommandSyntaxException {
        final net.minecraft.core.RegistryAccess.Frozen access = ((CraftServer) Bukkit.getServer()).getServer().registryAccess();
        var itemResult = new ItemParser(access).parse(new StringReader(item));
        return (CraftItemStack.asCraftMirror(new ItemInput(itemResult.item(), itemResult.components()).createItemStack(1, true)));
    }

    @Override
    public String formatItemStack(ItemStack itemStack) {
        return formatItemStack(itemStack, false);
    }

    @Override
    public ICommandSourceStack unwrapCommandSourceStack(io.papermc.paper.command.brigadier.CommandSourceStack commandSourceStack) {
        return CommandSourceStackImpl.of((net.minecraft.commands.CommandSourceStack) commandSourceStack);
    }

    @Override
    public CommandSyntaxException notPlayerException() {
        return CommandSourceStack.ERROR_NOT_PLAYER.create();
    }

    public String formatItemStack(org.bukkit.inventory.ItemStack item, boolean includeAllComponents) {
        var itemStringBuilder = new StringBuilder();
        var itemStack = CraftItemStack.asNMSCopy(item);
        final String itemName = itemStack.getItemHolder().unwrapKey().orElseThrow().location().toString();
        itemStringBuilder.append(itemName);
        final Set<DataComponentType<?>> referencedComponentTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        final DataComponentPatch patch = itemStack.getComponentsPatch();
        referencedComponentTypes.addAll(patch.entrySet().stream().map(Map.Entry::getKey).toList());
        final DataComponentMap prototype = itemStack.getItem().components();
        if (includeAllComponents) {
            referencedComponentTypes.addAll(prototype.keySet());
        }

        final RegistryAccess.Frozen access = ((CraftServer) Bukkit.getServer()).getServer().registryAccess();
        final RegistryOps<Tag> ops = access.createSerializationContext(NbtOps.INSTANCE);
        final Registry<DataComponentType<?>> registry = access.registryOrThrow(Registries.DATA_COMPONENT_TYPE);
        final List<ComponentLike> componentComponents = new ArrayList<>();
        final List<String> commandComponents = new ArrayList<>();
        for (final DataComponentType<?> type : referencedComponentTypes) {
            final String path = registry.getResourceKey(type).orElseThrow().location().getPath();
            final @Nullable Optional<?> patchedValue = patch.get(type);
            final @Nullable TypedDataComponent<?> prototypeValue = prototype.getTyped(type);
            if (patchedValue != null) {
                if (patchedValue.isEmpty()) {
                    commandComponents.add("!" + path);
                } else {
                    final Tag serialized = (Tag) ((DataComponentType) type).codecOrThrow().encodeStart(ops, patchedValue.get()).getOrThrow();
                    writeComponentValue(componentComponents::add, commandComponents::add, path, serialized);
                }
            } else if (includeAllComponents && prototypeValue != null) {
                final Tag serialized = prototypeValue.encodeValue(ops).getOrThrow();
                writeComponentValue(componentComponents::add, commandComponents::add, path, serialized);
            }
        }
        if (!componentComponents.isEmpty()) {
            itemStringBuilder
                    .append("[")
                    .append(String.join(",", commandComponents))
                    .append("]");
        }
        return itemStringBuilder.toString();
    }

    private static void writeComponentValue(final Consumer<Component> visualOutput, final Consumer<String> commandOutput, final String path, final Tag serialized) {
        visualOutput.accept(textOfChildren(
                text(path, color(0xFF7FD7)),
                text("=", WHITE),
                PaperAdventure.asAdventure(NbtUtils.toPrettyComponent(serialized))
        ));
        commandOutput.accept(path + "=" + new SnbtPrinterTagVisitor("", 0, new ArrayList<>()).visit(serialized));
    }

}
