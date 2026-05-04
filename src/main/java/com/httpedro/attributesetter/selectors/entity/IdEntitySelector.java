package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class IdEntitySelector extends EntitySelector {

    public ResourceLocation id;
    public IdEntitySelector(ResourceLocation id)
    {
        this.id = id;
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(obj.getType()).equals(id);
    }
    
    @Override
    public float getSpecificity() {
        return 50.0f;
    }
}

