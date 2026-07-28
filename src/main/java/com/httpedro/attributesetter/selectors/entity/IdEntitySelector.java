package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public class IdEntitySelector extends EntitySelector {

    public ResourceLocation id;
    public IdEntitySelector(ResourceLocation id)
    {
        this.id = id;
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return ForgeRegistries.ENTITY_TYPES.getKey(obj.getType()).equals(id);
    }

    @Override
    protected Boolean testTypeImpl(EntityType<?> type) {
        return id.equals(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    @Override
    public float getSpecificity() {
        return 50.0f;
    }
}
