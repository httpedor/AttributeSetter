package com.httpedro.attributesetter.ducktypes;

import net.minecraft.world.item.ItemStack;

/**
 * Exposes the (normally final) mutable properties of an {@link net.minecraft.world.item.Item} plus a couple of
 * extra fields the food setters need (custom eat duration and eaten-item conversion), which live on the item
 * in 1.20.1 rather than on the FoodProperties.
 */
public interface ItemDuckType {
    void as$setMaxStackSize(int maxStackSize);
    int as$getMaxStackSize();

    void as$setFoodProperties(net.minecraft.world.food.FoodProperties food);
    net.minecraft.world.food.FoodProperties as$getFoodProperties();

    /** Custom use duration in ticks, or null to use the vanilla value. */
    void as$setEatTicks(Integer ticks);
    Integer as$getEatTicks();

    /** Item this food turns into when fully eaten, or null for none. */
    void as$setConvertsTo(ItemStack stack);
    ItemStack as$getConvertsTo();
}
