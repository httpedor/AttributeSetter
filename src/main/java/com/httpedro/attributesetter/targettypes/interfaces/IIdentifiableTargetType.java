package com.httpedro.attributesetter.targettypes.interfaces;

import net.minecraft.resources.ResourceLocation;

public interface IIdentifiableTargetType<T> {
    ResourceLocation getId(T obj);
}
