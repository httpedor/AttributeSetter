package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.api.TrueDefaults;

import net.minecraft.world.item.ItemStack;

public class ItemDurabilitySetter extends ItemSetter{

    public int value;
    public float multiplier;
    public ItemDurabilitySetter(int value) {
        this.value = value;
        this.multiplier = 0;
    }
    public ItemDurabilitySetter(float multiplier) {
        this.value = 0;
        this.multiplier = multiplier;
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void apply(ItemStack target) {
        var item = target.getItem();
        TrueDefaults.snapshot(item);
        if (multiplier > 0)
            item.maxDamage = (int) (item.maxDamage * multiplier);
        else
            item.maxDamage = value;
    }

}
