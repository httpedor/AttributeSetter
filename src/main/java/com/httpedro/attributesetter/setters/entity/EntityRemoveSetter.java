package com.httpedro.attributesetter.setters.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Deletes an entity from the game: it never joins a level (so natural spawns, spawners, spawn eggs, breeding and
 * already-saved entities all fail), its spawn egg is removed like any other removed item, and the spawn placement
 * check denies it before the game even builds a candidate.
 *
 * <p>Most of the work is done by the removal handlers in {@code Attributesetter}, which look for this setter in the
 * entries matching an entity; {@link #apply} only covers the paths that already got an entity instance through.
 */
public class EntityRemoveSetter extends EntitySetter {
    @Override
    public void apply(LivingEntity target) {
        // Never let a selector like `isEnemy` or a regex take out the player.
        if (target instanceof Player)
            return;
        target.discard();
    }
}
