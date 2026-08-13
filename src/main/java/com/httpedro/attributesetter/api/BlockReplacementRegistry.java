package com.httpedro.attributesetter.api;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a block to the state every generated/placed copy of it should turn into. {@code remove} points a
 * block at air; {@code replace} points it at another block's default state.
 *
 * <p>Rebuilt from scratch on every datapack reload (and every client sync), so deleting the entry brings
 * the block back with no restore bookkeeping. The {@link #replace} lookup sits on the block-write hot
 * path ({@code setBlockState}), which runs on the server thread and on background worldgen threads at the
 * same time, hence the concurrent map and the {@link #active} fast-out so the common empty case is a
 * single field read.
 */
public class BlockReplacementRegistry {
    private static final Map<Block, BlockState> replacements = new ConcurrentHashMap<>();

    /** {@code false} whenever nothing is being replaced, so the setBlockState hooks bail out immediately. */
    public static volatile boolean active = false;

    public static void clear()
    {
        replacements.clear();
        active = false;
    }

    /** Points {@code from} at {@code to}'s default state. */
    public static void put(Block from, Block to)
    {
        if (from == null || to == null || from == to)
            return;
        replacements.put(from, to.defaultBlockState());
        active = true;
    }

    /** Points {@code block} at air, i.e. removes it from the world. */
    public static void remove(Block block)
    {
        if (block == null || block == Blocks.AIR)
            return;
        replacements.put(block, Blocks.AIR.defaultBlockState());
        active = true;
    }

    /** Returns the replacement state for {@code in}, or {@code in} itself when the block isn't replaced. */
    public static BlockState replace(BlockState in)
    {
        if (!active)
            return in;
        BlockState replacement = replacements.get(in.getBlock());
        return replacement != null ? replacement : in;
    }
}
