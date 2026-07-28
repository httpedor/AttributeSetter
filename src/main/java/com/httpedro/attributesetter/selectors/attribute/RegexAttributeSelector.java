package com.httpedro.attributesetter.selectors.attribute;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

public class RegexAttributeSelector extends AttributeSelector {
    public String regex;
    public RegexAttributeSelector(String regex)
    {
        this.regex = regex;
    }

    @Override
    protected boolean testImpl(Attribute obj) {
        var key = ForgeRegistries.ATTRIBUTES.getKey(obj);
        return key != null && key.toString().matches(regex);
    }

    @Override
    public float getSpecificity() {
        return 25;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
