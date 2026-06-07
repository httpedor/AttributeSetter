package com.httpedro.attributesetter.setters.itemstack.tooltip;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ItemTooltipAddSetter extends ItemStackTooltipSetter
{
    Component[] tooltip;
    public ItemTooltipAddSetter(Component[] tooltips) {
        super();
        this.tooltip = tooltips;
    }

    @Override
    public void apply(ItemTooltipEvent event) {
        for (Component component : tooltip) {
            event.getToolTip().add(component);
        }
    }
}
