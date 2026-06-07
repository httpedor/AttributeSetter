package com.httpedro.attributesetter.targettypes.interfaces;

import net.minecraft.core.component.DataComponentMap;

public interface IDataComponentHolderTargetType<T> {
    DataComponentMap getDataComponentMap(T obj);
}
