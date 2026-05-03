package com.httpedor.attributesetter.selectors.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class TagEntitySelector extends EntitySelector {
    public TagKey<EntityType<?>> tag;

    public TagEntitySelector(Identifier tag)
    {
        this.tag = TagKey.of(RegistryKeys.ENTITY_TYPE, tag);
    }

    public TagEntitySelector(TagKey<EntityType<?>> tag)
    {
        this.tag = tag;
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return obj.getType().isIn(tag);
    }

    @Override
    public float getSpecificity() {
        return 0;
    }
}