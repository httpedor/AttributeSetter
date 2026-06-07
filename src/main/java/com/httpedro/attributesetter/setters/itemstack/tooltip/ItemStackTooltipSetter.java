package com.httpedro.attributesetter.setters.itemstack.tooltip;

import com.httpedro.attributesetter.setters.ASEventSetter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public abstract class ItemStackTooltipSetter extends ASEventSetter<ItemStack, ItemTooltipEvent> {
    protected ItemStackTooltipSetter() {
        super(ItemTooltipEvent.class);
    }

    @Override
    public ItemStack getTarget(ItemTooltipEvent event) {
        return event.getItemStack();
    }
}
