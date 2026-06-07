package com.httpedro.attributesetter;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
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

