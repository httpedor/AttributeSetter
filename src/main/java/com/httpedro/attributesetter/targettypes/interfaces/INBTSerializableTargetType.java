package com.httpedro.attributesetter.targettypes.interfaces;

import java.util.function.Function;

import net.minecraft.nbt.Tag;

public interface INBTSerializableTargetType<T> {
    Function<T, Tag> getSerializer();
}
