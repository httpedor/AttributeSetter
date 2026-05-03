package com.httpedor.attributesetter.setters.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;

public class EntityAttributeSetter extends EntitySetter {
    public EntityAttribute attribute;
    public double value;

    public EntityAttributeSetter(EntityAttribute attribute, double value) {
        this.attribute = attribute;
        this.value = value;
    }

    @Override
    public void apply(LivingEntity target) {
        var instance = target.getAttributeInstance(attribute);
        if (instance != null)
            instance.setBaseValue(value);
    }
}