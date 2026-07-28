package com.httpedro.attributesetter.setters.item.tooltip;

import com.httpedro.attributesetter.setters.item.ItemSetter;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public abstract class ItemTooltipSetter extends ItemSetter {
    @Override
    public void apply(ItemStack target) {
        throw new UnsupportedOperationException("Use the apply method with ItemTooltipEvent");
    }

    public abstract void apply(ItemTooltipEvent event);
}
