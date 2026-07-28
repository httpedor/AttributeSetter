package com.httpedro.attributesetter.selectors.entity;

import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.CompositeASSelector;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves an entity selector into the entity types it matches, without needing an entity instance. Used by the
 * removal setter, which has to act before anything spawns (spawn eggs, spawn placement checks).
 */
public class EntityTypeResolver {
    /**
     * @return whether the selector matches the type, or {@code null} when it cannot be decided without a live
     *         entity (an NBT selector, or a composite containing one that would have decided the outcome).
     */
    public static Boolean matches(ASSelector<LivingEntity> selector, EntityType<?> type)
    {
        if (selector instanceof EntitySelector entitySelector)
            return entitySelector.testType(type);

        if (selector instanceof CompositeASSelector<LivingEntity> composite)
        {
            boolean undecided = false;
            for (var sub : composite.selectors)
            {
                var result = matches(sub, type);
                if (result == null)
                {
                    undecided = true;
                    continue;
                }
                // A short-circuiting sub-selector decides the composite even if another one is undecided.
                if (composite.mode == CompositeASSelector.Mode.AND && !result)
                    return invert(composite, false);
                if (composite.mode == CompositeASSelector.Mode.OR && result)
                    return invert(composite, true);
            }
            if (undecided)
                return null;
            return invert(composite, composite.mode == CompositeASSelector.Mode.AND);
        }

        return null;
    }

    /** Every entity type the selector is known to match. Types it cannot decide on are left out. */
    public static Set<EntityType<?>> resolve(ASSelector<LivingEntity> selector)
    {
        Set<EntityType<?>> types = new HashSet<>();
        for (var type : ForgeRegistries.ENTITY_TYPES.getValues())
        {
            var result = matches(selector, type);
            if (result != null && result)
                types.add(type);
        }
        return types;
    }

    private static boolean invert(ASSelector<?> selector, boolean value)
    {
        return selector.inverted != value;
    }
}
