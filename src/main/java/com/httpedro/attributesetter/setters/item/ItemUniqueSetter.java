package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.api.UniqueRegistry;

import net.minecraft.world.item.Item;

/**
 * The {@code item_type}-folder counterpart of {@link com.httpedro.attributesetter.setters.itemstack.ItemStackUniqueSetter}:
 * caps how many copies of an item may ever be created in the save. Applied per item at reload time (see
 * {@code DataReloader.load}); the loot/craft hooks do the enforcing via {@link UniqueRegistry}.
 */
public class ItemUniqueSetter extends ItemSetter {
    private final UniqueRegistry.Rule rule;

    public ItemUniqueSetter(UniqueRegistry.Rule rule) {
        this.rule = rule;
    }

    @Override
    public void apply(Item target) {
        UniqueRegistry.registerItem(target, rule);
    }
}
