package com.httpedro.attributesetter.setters.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class EntityAttributeSetter extends EntitySetter {
    public Attribute attribute;
    public double value;

    public EntityAttributeSetter(Attribute attribute, double value) {
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public void apply(LivingEntity target) {
        var instance = target.getAttribute(this.attribute);
        if (instance != null) {
            instance.setBaseValue(this.value);
        }
    }
    
}
