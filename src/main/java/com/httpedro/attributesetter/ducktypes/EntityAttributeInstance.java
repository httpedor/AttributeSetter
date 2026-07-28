package com.httpedro.attributesetter.ducktypes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;

public interface EntityAttributeInstance {
    LivingEntity getEntity();
    void setEntity(LivingEntity entity);
    Collection<AttributeModifier> getModifiersOrEmptyExposed(AttributeModifier.Operation operation, boolean countingInjection);
    void publicSetDirty();
}
