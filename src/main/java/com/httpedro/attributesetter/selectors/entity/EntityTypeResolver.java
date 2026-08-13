package com.httpedro.attributesetter.selectors.entity;

import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.CompositeASSelector;
import com.httpedro.attributesetter.selectors.IdSelector;
import com.httpedro.attributesetter.selectors.RegexSelector;
import com.httpedro.attributesetter.selectors.TagSelector;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves an entity selector into the entity types it matches, without an entity instance. Used by the removal
 * setter, which has to act before anything spawns (spawn eggs, spawn placement checks).
 *
 * <p>Entity selectors are built around extractors that take a live {@link LivingEntity}, so they can't simply be
 * fed an {@link EntityType}; instead the selectors whose answer depends only on the type are re-evaluated here
 * against the type. Anything else (NBT, {@code isEnemy}, a mod's own selector) is reported as undecided, which
 * only costs the type-level extras - the entity itself is still removed when it tries to join a level.
 */
public class EntityTypeResolver {
    /**
     * @return whether the selector matches the type, or {@code null} when it cannot be decided without a live
     *         entity (an NBT selector, or a composite containing one that would have decided the outcome).
     */
    public static Boolean matches(ASSelector<LivingEntity> selector, EntityType<?> type)
    {
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

        if (selector instanceof IdSelector<LivingEntity> idSelector)
            return invert(idSelector, idSelector.id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type)));

        if (selector instanceof RegexSelector<LivingEntity> regexSelector)
        {
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return invert(regexSelector, key != null && key.toString().matches(regexSelector.regex));
        }

        if (selector instanceof TagSelector<?, ?> tagSelector)
        {
            var tagKey = TagKey.create(BuiltInRegistries.ENTITY_TYPE.key(), tagSelector.tag);
            return invert(tagSelector, BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).is(tagKey));
        }

        if (selector instanceof IsMobCategorySelector categorySelector)
            return invert(categorySelector, type.getCategory() == categorySelector.category);

        return null;
    }

    /** Every entity type the selector is known to match. Types it cannot decide on are left out. */
    public static Set<EntityType<?>> resolve(ASSelector<LivingEntity> selector)
    {
        Set<EntityType<?>> types = new HashSet<>();
        for (var type : BuiltInRegistries.ENTITY_TYPE)
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
