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

    public BlockMiningSpeedSetter(float multiplier, float offset) {
        super(PlayerEvent.BreakSpeed.class);
        this.multiplier = multiplier;
        this.offset = offset;
    }

    @Override
    public void apply(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(Math.max(0.0F, event.getNewSpeed() * multiplier + offset));
    }

    @Override
    public Block getTarget(PlayerEvent.BreakSpeed event) {
        return event.getState().getBlock();
    }
}
