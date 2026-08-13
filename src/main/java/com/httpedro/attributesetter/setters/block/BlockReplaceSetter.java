package com.httpedro.attributesetter.setters.block;

import com.httpedro.attributesetter.api.BlockReplacementRegistry;

import net.minecraft.world.level.block.Block;

/**
 * Swaps a block for another one every time it is generated or placed (via the {@code setBlockState} hooks).
 * Like removal, blocks already in loaded chunks convert only when the game next writes that position.
 */
public class BlockReplaceSetter extends BlockSetter {
    private final Block replacement;

    public BlockReplaceSetter(Block replacement) {
        this.replacement = replacement;
    }

    @Override
    public void apply(Block target) {
        BlockReplacementRegistry.put(target, replacement);
    }
}
