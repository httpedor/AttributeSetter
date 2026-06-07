package com.httpedro.attributesetter.setters.attribute;

import com.httpedro.attributesetter.api.AttributeInjector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Collection;
import java.util.List;

public class AttributeModifiersInjectionSetter extends AttributeSetterAbsoluteCinema {
    public enum InjectionType
    {
        COPY,
        INJECT,
        BIDIRECTIONAL
    }
    ResourceLocation source;
    Collection<AttributeModifier.Operation> operations;
    float multiplier;
    InjectionType type;
    public AttributeModifiersInjectionSetter(ResourceLocation source, Collection<AttributeModifier.Operation> operations, InjectionType type, float multiplier)
    {
        this.source = source;
        this.operations = operations;
        this.type = type;
        this.multiplier = multiplier;
        if (this.operations == null || this.operations.isEmpty())
            this.operations = List.of(AttributeModifier.Operation.values());
    }

    @Override
    public void apply(Attribute target) {
        for (var op : operations)
        {
            if (type == InjectionType.COPY || type == InjectionType.BIDIRECTIONAL)
                AttributeInjector.registerInjectionFromOp(BuiltInRegistries.ATTRIBUTE.getKey(target), source, op, op, x -> x * multiplier);
            if (type == InjectionType.INJECT || type == InjectionType.BIDIRECTIONAL)
                AttributeInjector.registerInjectionFromOp(source, BuiltInRegistries.ATTRIBUTE.getKey(target), op, op, x -> x * multiplier);
        }
    }
}
