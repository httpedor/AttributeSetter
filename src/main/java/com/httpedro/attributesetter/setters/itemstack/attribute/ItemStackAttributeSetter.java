package com.httpedro.attributesetter.setters.itemstack.attribute;

import com.httpedro.attributesetter.setters.ASEventSetter;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public abstract class ItemStackAttributeSetter extends ASEventSetter<ItemStack, ItemAttributeModifierEvent> {
    protected ItemStackAttributeSetter() {
		super(ItemAttributeModifierEvent.class);
	}

	@Override
    public ItemStack getTarget(ItemAttributeModifierEvent event) {
        return event.getItemStack();
    }
}
