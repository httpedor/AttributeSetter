package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.api.RemovalRegistry;

import net.minecraft.world.item.ItemStack;

/**
 * Deletes an item from the game: creative tabs (and therefore JEI), every recipe, every loot table, item entities
 * lying in the world, player inventories and villager trades.
 *
 * <p>Global like the other item-wide setters, so it runs once per matching item at the end of a reload rather than
 * per stack.
 */
public class ItemRemoveSetter extends ItemSetter {
    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void apply(ItemStack target) {
        RemovalRegistry.removeItem(target.getItem());
    }
}
