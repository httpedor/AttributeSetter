package com.httpedro.attributesetter.setters.item.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class ItemTooltipAddSetter extends ItemTooltipSetter {
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
