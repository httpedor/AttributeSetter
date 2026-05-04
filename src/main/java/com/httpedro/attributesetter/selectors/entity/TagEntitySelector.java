package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class TagEntitySelector extends EntitySelector {

    public TagKey<EntityType<?>> tag;
    public TagEntitySelector(TagKey<EntityType<?>> tag)
    {
        this.tag = tag;
    }
    public TagEntitySelector(ResourceLocation tag)
    {
        this.tag = TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(), tag);
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return obj.getType().is(tag);
    }
    
    @Override
    public float getSpecificity() {
        return 0;
    }
}

