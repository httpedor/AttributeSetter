package com.httpedro.attributesetter.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the per-state {@code destroySpeed} (hardness) cache so the hardness setter can retune it.
 * Every {@link net.minecraft.world.level.block.state.BlockState} caches its own copy, so the setter has
 * to walk all of a block's states.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public interface BlockStateBaseAccessor {
    @Accessor("destroySpeed")
    float as$getDestroySpeed();

    @Accessor("destroySpeed")
    @Mutable
    void as$setDestroySpeed(float value);
}
