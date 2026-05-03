package com.httpedor.attributesetter.selectors.item;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

public class RegexItemSelector extends ItemSelector {
    public String regex;

    public RegexItemSelector(String regex)
    {
        this.regex = regex;
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        return Registries.ITEM.getId(obj.getItem()).toString().matches(regex);
    }

    @Override
    public float getSpecificity() {
        return 25;
    }
}