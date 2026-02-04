package com.httpedro.attributesetter.selectors.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class TagItemSelector extends ItemSelector {

    public TagKey<Item> tag;
    public TagItemSelector(ResourceLocation tag)
    {
        this.tag = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), tag);
    }
    public TagItemSelector(TagKey<Item> tag)
    {
        this.tag = tag;
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        return obj.is(tag);
    }
    
    @Override
    public float getSpecificity() {
        return 0;
    }
}
