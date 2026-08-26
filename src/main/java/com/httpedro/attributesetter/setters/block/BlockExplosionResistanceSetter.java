package com.httpedro.attributesetter.setters.block;

import com.httpedro.attributesetter.api.BlockDefaults;
import com.httpedro.attributesetter.mixin.BlockBehaviourAccessor;

import net.minecraft.world.level.block.Block;

/**
 * Retunes a block's blast resistance ({@code explosionResistance}) by {@code current * multiplier + offset}.
 * The field lives on the block itself (shared by every state), so one write covers the whole block.
 */
public class BlockExplosionResistanceSetter extends BlockSetter {
    private final float multiplier;
    private final float offset;
    /** Set when the entry gave a flat value instead of a tuning. */
    private final Float value;

    public BlockExplosionResistanceSetter(float multiplier, float offset) {
        this.multiplier = multiplier;
        this.offset = offset;
        this.value = null;
    }

    private BlockExplosionResistanceSetter(float value) {
        this.multiplier = 1;
        this.offset = 0;
        this.value = value;
    }

    /** A blast resistance written as a flat number. */
    public static BlockExplosionResistanceSetter absolute(float value) {
        return new BlockExplosionResistanceSetter(value);
    }

    @Override
    public void apply(Block target) {
        BlockDefaults.markModified(target);
        var accessor = (BlockBehaviourAccessor) target;
        float result = value != null ? value : accessor.as$getExplosionResistance() * multiplier + offset;
        accessor.as$setExplosionResistance(Math.max(0.0F, result));
    }
}
