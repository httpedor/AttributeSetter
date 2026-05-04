package com.httpedro.attributesetter.setters.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public abstract class ItemAttributeSetter extends ItemSetter {
    @Override
    public void apply(ItemStack target) {
        throw new UnsupportedOperationException("Use the apply method with ItemAttributeModifierEvent");
    }

    public abstract void apply(ItemAttributeModifierEvent e);
}

