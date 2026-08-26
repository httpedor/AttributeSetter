package com.httpedro.attributesetter.api;

import com.httpedro.attributesetter.mixin.BlockBehaviourAccessor;
import com.httpedro.attributesetter.mixin.BlockStateBaseAccessor;

import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import com.httpedro.attributesetter.util.RegistryValues;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

/**
 * Vanilla snapshot of the block fields the block setters mutate in place - hardness ({@code destroySpeed})
 * and blast resistance ({@code explosionResistance}). Both come from a single {@code Properties} value per
 * block, so one float each captures the whole block (the hardness restore still has to walk every state,
 * since each caches its own copy).
 *
 * <p>Mirrors {@link TrueDefaults} for items: snapshotted once, and only the blocks a reload actually
 * touched are reverted on the next reload, so removing an entry restores the vanilla value.
 */
public class BlockDefaults {
    private static Reference2FloatOpenHashMap<Block> hardness;
    private static Reference2FloatOpenHashMap<Block> blastResistance;
    private static final Set<Block> modified = new HashSet<>();

    public static void populate()
    {
        hardness = new Reference2FloatOpenHashMap<>();
        blastResistance = new Reference2FloatOpenHashMap<>();
        for (var block : RegistryValues.of(BuiltInRegistries.BLOCK))
        {
            hardness.put(block, ((BlockStateBaseAccessor) block.defaultBlockState()).as$getDestroySpeed());
            blastResistance.put(block, ((BlockBehaviourAccessor) block).as$getExplosionResistance());
        }
    }

    public static boolean isPopulated() {
        return hardness != null;
    }

    public static float getHardness(Block block) {
        return hardness.getFloat(block);
    }

    public static float getBlastResistance(Block block) {
        return blastResistance.getFloat(block);
    }

    /** Flags a block as touched this reload so it gets reverted on the next one. */
    public static void markModified(Block block) {
        modified.add(block);
    }

    /** Restores every block a previous reload changed back to its vanilla fields, then forgets them. */
    public static void restoreModified()
    {
        if (modified.isEmpty())
            return;
        for (var block : modified)
        {
            ((BlockBehaviourAccessor) block).as$setExplosionResistance(blastResistance.getFloat(block));
            float base = hardness.getFloat(block);
            for (var state : block.getStateDefinition().getPossibleStates())
                ((BlockStateBaseAccessor) state).as$setDestroySpeed(base);
        }
        modified.clear();
    }
}
