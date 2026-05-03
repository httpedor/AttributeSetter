package com.httpedor.attributesetter.selectors.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;

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
        return Registries.ENTITY_TYPE.getId(obj.getType()).toString().matches(regex);
    }
}