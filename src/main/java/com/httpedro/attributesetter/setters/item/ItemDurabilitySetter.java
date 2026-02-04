package com.httpedro.attributesetter.setters.item;

import net.minecraft.world.item.ItemStack;

public class ItemDurabilitySetter extends ItemSetter{

    public int value;
    public ItemDurabilitySetter(int value) {
        this.value = value;
    }

    @Override
    public void apply(ItemStack target) {
        target.getItem().maxDamage = value;
    }
    
}
