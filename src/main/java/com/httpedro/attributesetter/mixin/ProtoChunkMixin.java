package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.api.BlockReplacementRegistry;

import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The worldgen counterpart of {@link LevelChunkMixin}: chunks are still being generated write through
 * {@link ProtoChunk#setBlockState}, so this is where {@code remove}/{@code replace} catch ores, structures
 * and terrain features before the chunk is ever loaded.
 */
@Mixin(ProtoChunk.class)
public class ProtoChunkMixin {
    @ModifyVariable(
            method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private BlockState as$replaceBlock(BlockState state) {
        return BlockReplacementRegistry.replace(state);
    }
}
