package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;

public class RegexEntitySelector extends EntitySelector {
    public String regex;
    public RegexEntitySelector(String regex)
    {
        this.regex = regex;
    }

    @Override
    public float getSpecificity() {
        return 25;
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(obj.getType()).toString().matches(regex);
    }
    
}

