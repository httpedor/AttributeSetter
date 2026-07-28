package com.httpedro.attributesetter.selectors.item;

import net.minecraft.world.item.ItemStack;

public class HasDurabilityItemSelector extends ItemSelector {

    @Override
    protected boolean testImpl(ItemStack obj) {
        return obj.isDamageableItem();
    }

    @Override
    public float getSpecificity() {
        return 0;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
