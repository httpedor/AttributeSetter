package com.httpedro.attributesetter.setters.itemstack.tooltip;

import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class ItemTooltipModifySetter extends ItemStackTooltipSetter {
    public enum Type {
        INSERT,
        REPLACE,
        REMOVE
    }
    final Component[] components;
    final int index;
    final Type type;
    public ItemTooltipModifySetter(int index, Type type, Component... components)
    {
        super();
        this.index = index;
        this.type = type;
        this.components = components;
    }
    @Override
    public void apply(ItemTooltipEvent event) {
        switch (type) {
            case INSERT -> {
                for (int i = 0; i < components.length; i++) {
                    event.getToolTip().add(index + i, components[i]);
                }
            }
            case REPLACE -> {
                for (int i = 0; i < components.length; i++) {
                    if (index + i < event.getToolTip().size()) {
                        event.getToolTip().set(index + i, components[i]);
                    } else {
                        event.getToolTip().add(components[i]);
                    }
                }
            }
            case REMOVE -> {
                for (int i = 0; i < components.length; i++) {
                    if (index + i < event.getToolTip().size()) {
                        event.getToolTip().remove(index + i);
                    }
                }
            }
        }
    }
}
