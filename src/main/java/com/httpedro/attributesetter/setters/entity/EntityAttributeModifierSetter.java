package com.httpedro.attributesetter.setters.entity;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class EntityAttributeModifierSetter extends EntitySetter {
    public Holder<Attribute> attribute;
    public AttributeModifier modifier;

    public EntityAttributeModifierSetter(Holder<Attribute> attribute, AttributeModifier.Operation op, double amount, String uniqueIndex) {
        this.modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex), amount, op);
        this.attribute = attribute;
    }

    @Override
    public void apply(LivingEntity target) {
        var instance = target.getAttribute(this.attribute);
        if (instance != null) {
            if (instance.hasModifier(modifier.id()))
                instance.removeModifier(modifier);
            instance.addPermanentModifier(this.modifier);
        }
    }
    
}


