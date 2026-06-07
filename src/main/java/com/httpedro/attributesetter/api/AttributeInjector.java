package com.httpedro.attributesetter.api;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.ducktypes.EntityAttributeInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.*;
import java.util.function.Function;

public class AttributeInjector {
    public static class InjectionSource
    {
        public ResourceLocation sourceAttribute;
        public Function<Float, Float> conversion;
        public InjectionSource(ResourceLocation sourceAttribute, Function<Float, Float> conversion) {
            this.sourceAttribute = sourceAttribute;
            this.conversion = conversion;
        }
    }
    public static class InjectionSourceWithOp extends InjectionSource
    {
        public AttributeModifier.Operation sourceOp;
        public InjectionSourceWithOp(ResourceLocation sourceAttribute, AttributeModifier.Operation sourceOp, Function<Float, Float> conversion) {
            super(sourceAttribute, conversion);
            this.sourceOp = sourceOp;
        }
    }
    // note: It's inverted because of efficiency reasons. In the entry, the data is better organized that way.
    // Injected -> Which Operation on the Injected -> List of (Conversion function, Operation on the source attribute, Source)
    private static final Map<ResourceLocation, Map<AttributeModifier.Operation, Collection<InjectionSourceWithOp>>> mappedEntries = new HashMap<>();
    // Source -> Injected. Used for updating dirty attributes.
    private static final Map<ResourceLocation, Collection<ResourceLocation>> attributeDependencies = new HashMap<>();
    // Entries executed on the base value of the source attribute, organized by the resulting operation
    private static final Map<ResourceLocation, Map<AttributeModifier.Operation, Collection<InjectionSource>>> baseEntries = new HashMap<>();
    // Same as above but for the final value of the source attribute
    private static final Map<ResourceLocation, Map<AttributeModifier.Operation, Collection<InjectionSource>>> finalEntries = new HashMap<>();

    public static Collection<AttributeModifier> getInjectionsFor(ResourceLocation attrId, AttributeModifier.Operation op, LivingEntity entity)
    {
        var modifiers = new LinkedList<AttributeModifier>();
        int i = 0;
        for (var entry : getEntriesForAttributeOp(attrId, op))
        {
            var otherAttrId = entry.sourceAttribute;
            var otherAttr = BuiltInRegistries.ATTRIBUTE.getHolder(otherAttrId).orElse(null);
            if (otherAttr == null)
                continue;

            var otherAttrInstance = entity.getAttribute(otherAttr);
            if (otherAttrInstance == null)
                continue;

            var conversion = entry.conversion;
            if (conversion != null)
            {
                for (var mod : ((EntityAttributeInstance)otherAttrInstance).getModifiersOrEmptyExposed(entry.sourceOp, false)) {
                    var converted = conversion.apply((float)mod.amount());
                    modifiers.add(new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(Attributesetter.MODID, "attr_copy_" + otherAttrId.getPath() + "/" + mod.id().toString().replace(':', '/') + "/" + i),
                            converted, op));
                }
            }
            i++;
        }
        i = 0;
        for (var entry : getBaseEntriesForAttributeOp(attrId, op))
        {
            var otherAttrId = entry.sourceAttribute;
            var otherAttr = BuiltInRegistries.ATTRIBUTE.getHolder(otherAttrId).orElse(null);
            if (otherAttr == null)
                continue;

            var otherAttrInstance = entity.getAttribute(otherAttr);
            if (otherAttrInstance == null)
                continue;

            var conversion = entry.conversion;
            if (conversion != null)
            {
                var converted = conversion.apply((float)otherAttrInstance.getBaseValue());
                modifiers.add(new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(Attributesetter.MODID, "attr_copy_base_" + otherAttrId.getPath() + "/" + i),
                        converted, op));
            }
        }
        for (var entry : getFinalEntriesForAttributeOp(attrId, op))
        {
            var otherAttrId = entry.sourceAttribute;
            var otherAttr = BuiltInRegistries.ATTRIBUTE.getHolder(otherAttrId).orElse(null);
            if (otherAttr == null)
                continue;

            var otherAttrInstance = entity.getAttribute(otherAttr);
            if (otherAttrInstance == null)
                continue;

            var conversion = entry.conversion;
            if (conversion != null)
            {
                var converted = conversion.apply((float)otherAttrInstance.getValue());
                modifiers.add(new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(Attributesetter.MODID, "attr_copy_final_" + otherAttrId.getPath() + "/" + i),
                        converted, op));
            }
        }

        return modifiers;
    }
    public static Collection<Holder<Attribute>> getAttributesDependentOn(ResourceLocation attrId)
    {
        var result = new LinkedList<Holder<Attribute>>();
        var dependents = attributeDependencies.get(attrId);
        if (dependents != null) {
            for (var dependent : dependents)
            {
                var attr = BuiltInRegistries.ATTRIBUTE.getHolder(dependent).orElse(null);
                if (attr != null)
                    result.add(attr);
            }
        }
        return result;
    }

    public static Collection<InjectionSourceWithOp> getEntriesForAttributeOp(ResourceLocation attribute, AttributeModifier.Operation op)
    {
        var opEntries = mappedEntries.get(attribute);
        if (opEntries != null)
        {
            var entries = opEntries.get(op);
            if (entries != null)
                return entries;
        }
        return Collections.emptyList();
    }

    public static Collection<InjectionSource> getBaseEntriesForAttributeOp(ResourceLocation attribute, AttributeModifier.Operation op)
    {
        var opEntries = baseEntries.get(attribute);
        if (opEntries != null)
        {
            var entries = opEntries.get(op);
            if (entries != null)
                return entries;
        }
        return Collections.emptyList();
    }
    public static Collection<InjectionSource> getFinalEntriesForAttributeOp(ResourceLocation attribute, AttributeModifier.Operation op)
    {
        var opEntries = finalEntries.get(attribute);
        if (opEntries != null)
        {
            var entries = opEntries.get(op);
            if (entries != null)
                return entries;
        }
        return Collections.emptyList();
    }

    public static void registerInjectionFromOp(ResourceLocation targetAttribute, ResourceLocation sourceAttribute, AttributeModifier.Operation sourceOp, AttributeModifier.Operation targetOp, Function<Float, Float> conversion)
    {
        mappedEntries.computeIfAbsent(targetAttribute, k -> new HashMap<>()).computeIfAbsent(targetOp, k -> new LinkedList<>()).add(new InjectionSourceWithOp(sourceAttribute, sourceOp, conversion));
        attributeDependencies.computeIfAbsent(sourceAttribute, k -> new LinkedList<>()).add(targetAttribute);
    }
    public static void registerInjectionFromBase(ResourceLocation targetAttribute, ResourceLocation sourceAttribute, AttributeModifier.Operation targetOp, Function<Float, Float> conversion)
    {
        baseEntries.computeIfAbsent(targetAttribute, k -> new HashMap<>()).computeIfAbsent(targetOp, k -> new LinkedList<>()).add(new InjectionSource(sourceAttribute, conversion));
        attributeDependencies.computeIfAbsent(sourceAttribute, k -> new LinkedList<>()).add(targetAttribute);
    }
    public static void registerInjectionFromFinal(ResourceLocation targetAttribute, ResourceLocation sourceAttribute, AttributeModifier.Operation targetOp, Function<Float, Float> conversion) {
        finalEntries.computeIfAbsent(targetAttribute, k -> new HashMap<>()).computeIfAbsent(targetOp, k -> new LinkedList<>()).add(new InjectionSource(sourceAttribute, conversion));
        attributeDependencies.computeIfAbsent(sourceAttribute, k -> new LinkedList<>()).add(targetAttribute);
    }

    public static void clearAll() {
        mappedEntries.clear();
        attributeDependencies.clear();
        baseEntries.clear();
        finalEntries.clear();
    }
}
