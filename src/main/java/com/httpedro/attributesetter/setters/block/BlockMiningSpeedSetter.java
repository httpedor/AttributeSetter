package com.httpedro.attributesetter.setters.block;

import com.httpedro.attributesetter.setters.ASEventSetter;

import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Retunes how fast a player digs a matching block by {@code current * multiplier + offset} on the dig
 * speed. Unlike hardness this never touches the block - it rides the {@link PlayerEvent.BreakSpeed} event,
 * so it's per-player and dynamic, and it stacks with any other break-speed modifiers already applied.
 */
public class BlockMiningSpeedSetter extends ASEventSetter<Block, PlayerEvent.BreakSpeed> {
    private final float multiplier;
    private final float offset;
    /** Set when the entry gave a flat dig speed instead of a tuning. */
    private final Float value;

    public BlockMiningSpeedSetter(float multiplier, float offset) {
        super(PlayerEvent.BreakSpeed.class);
        this.multiplier = multiplier;
        this.offset = offset;
        this.value = null;
    }

    private BlockMiningSpeedSetter(float value) {
        super(PlayerEvent.BreakSpeed.class);
        this.multiplier = 1;
        this.offset = 0;
        this.value = value;
    }

    /** A dig speed written as a flat number, replacing whatever the tool/player would have given. */
    public static BlockMiningSpeedSetter absolute(float value) {
        return new BlockMiningSpeedSetter(value);
    }

    @Override
    public void apply(PlayerEvent.BreakSpeed event) {
        float result = value != null ? value : event.getNewSpeed() * multiplier + offset;
        event.setNewSpeed(Math.max(0.0F, result));
    }

    @Override
    public Block getTarget(PlayerEvent.BreakSpeed event) {
        return event.getState().getBlock();
    }
}
