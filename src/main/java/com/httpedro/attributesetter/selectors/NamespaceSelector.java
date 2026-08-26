package com.httpedro.attributesetter.selectors;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

/** Matches everything from one mod: {@code {"namespace": "minecraft"}} or {@code "@minecraft"}. */
public class NamespaceSelector<T> extends ASSelector<T> {
    public final String namespace;
    public final Function<T, ResourceLocation> idGetter;

    public NamespaceSelector(String namespace, Function<T, ResourceLocation> idGetter) {
        this.namespace = namespace;
        this.idGetter = idGetter;
    }

    @Override
    protected boolean testImpl(T obj) {
        var id = idGetter.apply(obj);
        return id != null && id.getNamespace().equals(namespace);
    }

    @Override
    public float getSpecificity() {
        return 10;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
