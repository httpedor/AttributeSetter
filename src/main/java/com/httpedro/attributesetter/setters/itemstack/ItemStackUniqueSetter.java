package com.httpedro.attributesetter.setters.itemstack;

import com.httpedro.attributesetter.api.UniqueRegistry;

import net.minecraft.world.item.ItemStack;

/**
 * The {@code item}-folder (itemstack) counterpart of the {@code make_unique} setter: caps how many copies of an item
 * may ever be created in the save. Like {@link ItemStackRemoveSetter} it is not an event setter, so it is collected
 * once at reload time by probing every item (see {@code DataReloader.load}); {@link #apply} records the item's cap
 * into {@link UniqueRegistry} and the loot/craft hooks do the enforcing.
 */
public class ItemStackUniqueSetter extends ItemStackSetter {
    private final UniqueRegistry.Rule rule;

    public ItemStackUniqueSetter(UniqueRegistry.Rule rule) {
        this.rule = rule;
    }

    @Override
    public void apply(ItemStack target) {
        UniqueRegistry.registerItem(target.getItem(), rule);
    }
}
