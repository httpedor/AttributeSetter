package com.httpedro.attributesetter.selectors.entity;

import com.httpedro.attributesetter.selectors.ASSelector;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public abstract class EntitySelector extends ASSelector<LivingEntity>{

    /**
     * Type-level evaluation, used by removals to resolve the selector into entity types before any entity exists
     * (spawn eggs, spawn placement checks). Return {@code null} when the selector needs a live entity to answer,
     * like the NBT one.
     */
    protected Boolean testTypeImpl(EntityType<?> type)
    {
        return null;
    }

    /** @see #testTypeImpl(EntityType) */
    public final Boolean testType(EntityType<?> type)
    {
        var ret = testTypeImpl(type);
        if (ret == null)
            return null;
        return inverted ? !ret : ret;
    }
}
