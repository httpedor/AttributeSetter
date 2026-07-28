package com.httpedro.attributesetter.selectors.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

public class IsPotionItemSelector extends ItemSelector {

    @Override
    protected boolean testImpl(ItemStack obj) {
        // Matches potions, splash/lingering potions and tipped arrows that actually carry a potion.
        return !PotionUtils.getPotion(obj).equals(Potions.EMPTY) || !PotionUtils.getCustomEffects(obj).isEmpty();
    }

    @Override
    public float getSpecificity() {
        return 0;
    }

    // Potion contents live in NBT, so results must not be cached per Item.
    @Override
    public boolean canCache() {
        return false;
    }
}
