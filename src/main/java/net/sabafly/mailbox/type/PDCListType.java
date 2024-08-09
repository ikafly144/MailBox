package net.sabafly.mailbox.type;

import com.google.common.collect.Lists;
import org.bukkit.persistence.ListPersistentDataType;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PDCListType <P, C> implements ListPersistentDataType<P, C> {
    private final PersistentDataType<P, C> innerType;

    public PDCListType(@NotNull final PersistentDataType<P, C> innerType) {
        this.innerType = innerType;
    }

    @Override
    public @NotNull PersistentDataType<P, C> elementType() {
        return this.innerType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Class<List<P>> getPrimitiveType() {
        return (Class<List<P>>) (Object) List.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Class<List<C>> getComplexType() {
        return (Class<List<C>>) (Object) List.class;
    }

    @NotNull
    @Override
    public List<P> toPrimitive(@NotNull List<C> complex, @NotNull PersistentDataAdapterContext context) {
        return Lists.transform(complex, c -> innerType.toPrimitive(c, context));
    }

    @NotNull
    @Override
    public List<C> fromPrimitive(@NotNull List<P> primitive, @NotNull PersistentDataAdapterContext context) {
        return Lists.transform(primitive, p -> innerType.fromPrimitive(p, context));
    }
}
