package com.httpedro.attributesetter.setters.entity;

import com.httpedro.attributesetter.api.UniqueRegistry;

import net.minecraft.world.entity.LivingEntity;

/**
 * Caps how many of an entity type may ever spawn in the save. Like {@link EntityRemoveSetter} this is a marker: the
 * actual gating lives in {@code Attributesetter.processEntity} (which cancels over-cap spawns and marks the counted
 * ones persistent), and the cap is registered against each resolvable entity type at reload time (see
 * {@code DataReloader.load}) via {@link com.httpedro.attributesetter.selectors.entity.EntityTypeResolver}.
 *
 * <p>{@link #apply} is never called directly for gating; it only carries the {@link UniqueRegistry.Rule} for the
 * reload pass to read.
 */
public class EntityUniqueSetter extends EntitySetter {
    private final UniqueRegistry.Rule rule;

    public EntityUniqueSetter(UniqueRegistry.Rule rule) {
        this.rule = rule;
    }

    public UniqueRegistry.Rule getRule() {
        return rule;
    }

    @Override
    public void apply(LivingEntity target) {
        // No-op: registration happens at reload, gating happens in Attributesetter.processEntity.
    }
}
