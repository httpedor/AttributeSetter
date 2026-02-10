package com.httpedro.attributesetter.selectors.item;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class RegexItemSelector extends ItemSelector {
    public String regex;
    public RegexItemSelector(String regex)
    {
        this.regex = regex;
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        return ForgeRegistries.ITEMS.getKey(obj.getItem()).toString().matches(regex);
    }

    @Override
    public float getSpecificity() {
        return 25;
    }
    
}
