package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.setters.ASSetter;

import net.minecraft.world.item.ItemStack;

public abstract class ItemSetter extends ASSetter<ItemStack> {
    /**
     * Global setters mutate the shared {@link net.minecraft.world.item.Item} (max stack, durability, food)
     * rather than a single stack, so they are applied once per matching item at reload time instead of
     * per-stack inside item events.
     */
    public boolean isGlobal() {
        return false;
    }
}
