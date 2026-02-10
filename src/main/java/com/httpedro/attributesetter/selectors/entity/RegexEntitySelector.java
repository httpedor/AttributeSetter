package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

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
        return ForgeRegistries.ENTITY_TYPES.getKey(obj.getType()).toString().matches(regex);
    }
    
}
