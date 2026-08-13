package com.httpedro.attributesetter.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the block-level {@code explosionResistance} field so the blast-resistance setter can retune it.
 * The field is shared by every state of a block, so a single write covers the whole block.
 */
@Mixin(BlockBehaviour.class)
public interface BlockBehaviourAccessor {
    @Accessor("explosionResistance")
    float as$getExplosionResistance();

    @Accessor("explosionResistance")
    @Mutable
    void as$setExplosionResistance(float value);
}
