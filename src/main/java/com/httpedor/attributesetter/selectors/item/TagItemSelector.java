package com.httpedor.attributesetter.selectors.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class TagItemSelector extends ItemSelector {
    public TagKey<Item> tag;

    public TagItemSelector(Identifier tag)
    {
        this.tag = TagKey.of(RegistryKeys.ITEM, tag);
    }

    public TagItemSelector(TagKey<Item> tag)
    {
        this.tag = tag;
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        return obj.isIn(tag);
    }

    @Override
    public float getSpecificity() {
        return 0;
    }
}