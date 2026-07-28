package com.httpedro.attributesetter;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;

public interface ASLivingEntity
{
    void as$addInjection(TemporaryAttributeInjection injection);
    Collection<TemporaryAttributeInjection> as$getInjections();
    Collection<TemporaryAttributeInjection> as$getInjections(AttributeModifier.Operation operation);
    boolean as$isLoaded();
    void as$setLoaded();
}
