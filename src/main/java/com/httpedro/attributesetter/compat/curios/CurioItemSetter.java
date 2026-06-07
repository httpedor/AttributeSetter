package com.httpedro.attributesetter.compat.curios;

import com.httpedro.attributesetter.setters.ASEventSetter;

import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public abstract class CurioItemSetter extends ASEventSetter<ItemStack, CurioAttributeModifierEvent> {

	protected CurioItemSetter() {
		super(CurioAttributeModifierEvent.class);
	}

}
