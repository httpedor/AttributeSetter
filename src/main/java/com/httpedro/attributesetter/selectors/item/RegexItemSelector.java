package com.httpedro.attributesetter.selectors.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public class RegexItemSelector extends ItemSelector {
    public String regex;
    public RegexItemSelector(String regex)
    {
        this.regex = regex;
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        return BuiltInRegistries.ITEM.getKey(obj.getItem()).toString().matches(regex);
    }

    @Override
    public float getSpecificity() {
        return 25;
    }
    
}

