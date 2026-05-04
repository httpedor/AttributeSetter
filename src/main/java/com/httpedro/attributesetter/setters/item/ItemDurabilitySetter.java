package com.httpedro.attributesetter.setters.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilitySetter extends ItemSetter{

    public int value;
    public ItemDurabilitySetter(int value) {
        this.value = value;
    }

    @Override
    public void apply(ItemStack target) {
        target.set(DataComponents.MAX_DAMAGE, value);
    }
    
}

