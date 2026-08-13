package com.httpedro.attributesetter.setters.block;

import com.httpedro.attributesetter.api.BlockReplacementRegistry;
import com.httpedro.attributesetter.api.RemovalRegistry;

import net.minecraft.world.level.block.Block;

/**
 * Deletes a block from the game: every generated or placed copy turns into air (via the
 * {@code setBlockState} hooks), and its BlockItem is removed like any other removed item, so it also
 * leaves the creative inventory, recipes, loot and inventories.
 *
 * <p>Blocks already sitting in loaded chunks aren't retroactively cleared - they convert the next time
 * the game writes that position - the same limitation removed entities have for already-saved mobs.
 */
public class BlockRemoveSetter extends BlockSetter {
    @Override
    public void apply(Block target) {
        BlockReplacementRegistry.remove(target);
        RemovalRegistry.removeItem(target.asItem());
    }
}
