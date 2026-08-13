package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.api.BlockReplacementRegistry;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Routes every block written into a loaded chunk through {@link BlockReplacementRegistry}, so {@code remove}
 * and {@code replace} catch player/mob/dispenser placement, pistons, fluids and anything else that touches
 * a live chunk. The registry's {@code active} flag short-circuits to a single field read when nothing is
 * being replaced.
 */
@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @ModifyVariable(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private BlockState as$replaceBlock(BlockState state) {
        return BlockReplacementRegistry.replace(state);
    }
}
