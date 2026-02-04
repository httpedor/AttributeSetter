package com.httpedro.attributesetter.setters.entity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class EntityAttributeModifierSetter extends EntitySetter {
    public Attribute attribute;
    public AttributeModifier modifier;

    public EntityAttributeModifierSetter(Attribute attribute, AttributeModifier.Operation op, double amount, String uniqueIndex) {
        UUID id = UUID.nameUUIDFromBytes(uniqueIndex.getBytes(StandardCharsets.UTF_8));
        this.modifier = new AttributeModifier(id, uniqueIndex, amount, op);
        this.attribute = attribute;
    }

    @Override
    public void apply(LivingEntity target) {
        var instance = target.getAttribute(this.attribute);
        if (instance != null) {
            if (instance.hasModifier(modifier))
                instance.removeModifier(modifier);
            instance.addPermanentModifier(this.modifier);
        }
    }
    
}

