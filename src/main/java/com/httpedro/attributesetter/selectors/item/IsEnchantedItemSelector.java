package com.httpedro.attributesetter.selectors.item;

import net.minecraft.world.item.ItemStack;

public class IsEnchantedItemSelector extends ItemSelector {

    @Override
    protected boolean testImpl(ItemStack obj) {
        return obj.isEnchanted();
    }

    @Override
    public float getSpecificity() {
        return 0;
    }

    // Enchantment state is per-stack, so results must not be cached per Item.
    @Override
    public boolean canCache() {
        return false;
    }
}
