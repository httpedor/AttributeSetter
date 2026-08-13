package com.httpedro.attributesetter.setters.block;

import com.httpedro.attributesetter.api.BlockDefaults;
import com.httpedro.attributesetter.mixin.BlockStateBaseAccessor;

import net.minecraft.world.level.block.Block;

/**
 * Retunes a block's hardness ({@code destroySpeed}) by {@code current * multiplier + offset}. The value is
 * cached per state, so every state of the block is rewritten; reading the live value means several
 * hardness entries on the same block stack in selector-specificity order.
 */
public class BlockHardnessSetter extends BlockSetter {
    private final float multiplier;
    private final float offset;

    public BlockHardnessSetter(float multiplier, float offset) {
        this.multiplier = multiplier;
        this.offset = offset;
    }

    @Override
    public void apply(Block target) {
        BlockDefaults.markModified(target);
        for (var state : target.getStateDefinition().getPossibleStates())
        {
            var accessor = (BlockStateBaseAccessor) state;
            float current = accessor.as$getDestroySpeed();
            // -1 is the vanilla "unbreakable" sentinel (bedrock, barrier, ...). Leave those alone rather
            // than let the offset/multiplier math turn them into instantly-breakable blocks.
            if (current < 0)
                continue;
            accessor.as$setDestroySpeed(Math.max(0.0F, current * multiplier + offset));
        }
    }
}
