package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.api.RemovalRegistry;

import net.minecraft.world.item.Item;

/**
 * Deletes an item from the game: creative tabs (and therefore JEI), every recipe, every loot table, item entities
 * lying in the world, player inventories and villager trades.
 *
 * <p>Unlike the other item setters this one doesn't patch the item's components, it just records the item - the
 * removal handlers do the rest, which is why nothing has to be restored when the entry goes away.
 */
public class ItemRemoveSetter extends ItemSetter {
    @Override
    public void apply(Item target) {
        RemovalRegistry.removeItem(target);
    }
}
