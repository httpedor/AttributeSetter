package com.httpedro.attributesetter.selectors;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

public class IdSelector<T> extends ASSelector<T> {
    public ResourceLocation id;
    public Function<T, ResourceLocation> idGetter;

    public IdSelector(ResourceLocation id, Function<T, ResourceLocation> idGetter) {
        this.id = id;
        this.idGetter = idGetter;
    }

    @Override
    protected boolean testImpl(T obj) {
        return id.equals(idGetter.apply(obj));
    }

    @Override
    public float getSpecificity() {
        return 50;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
