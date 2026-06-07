package com.httpedro.attributesetter.targettypes.interfaces;

import net.minecraft.core.Registry;

public interface IRegistryAssociatedTargetType<T> {
    Registry<T> getRegistry();
}
