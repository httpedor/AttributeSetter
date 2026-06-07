package com.httpedro.attributesetter.targettypes;

import com.httpedro.attributesetter.api.TargetType;

import com.httpedro.attributesetter.targettypes.interfaces.IIdentifiableTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IRegistryAssociatedTargetType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public abstract class RegistryTargetType<TInst, TSing> extends TargetType<TInst, TSing> implements IIdentifiableTargetType<TInst>, IRegistryAssociatedTargetType<TSing> {

    protected Registry<TSing> registry;

    public RegistryTargetType(Class<TInst> registryClass, Registry<TSing> registry) {
        super(registryClass);
        this.registry = registry;
    }

    public Registry<TSing> getRegistry() {
        return registry;
    }

    public abstract TSing getSingleton(TInst instance);

    @Override
    public TSing getCacheKey(TInst object) {
        return getSingleton(object);
    }

    @Override
    public ResourceLocation getId(TInst obj) {
        return registry.getKey(getSingleton(obj));
    }
}
