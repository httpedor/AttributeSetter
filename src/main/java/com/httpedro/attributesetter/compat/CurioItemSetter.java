package com.httpedro.attributesetter.compat;

import com.httpedro.attributesetter.setters.item.ItemSetter;

import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public abstract class CurioItemSetter extends ItemSetter{

    @Override
    public void apply(ItemStack target) {
        throw new UnsupportedOperationException("CurioItemSetter does not support direct application to ItemStack");
    }

    @Override
    public boolean shouldApply(ItemStack target) {
        return false;
    }
    
    public abstract void apply(CurioAttributeModifierEvent e);
}

