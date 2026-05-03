package com.httpedor.attributesetter.setters.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;

public class EntityAttributeModifierSetter extends EntitySetter {
    public EntityAttribute attribute;
    public EntityAttributeModifier modifier;

    public EntityAttributeModifierSetter(EntityAttribute attribute, EntityAttributeModifier.Operation op, double amount, String uniqueIndex) {
        java.util.UUID id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.modifier = new EntityAttributeModifier(id, uniqueIndex, amount, op);
        this.attribute = attribute;
    }

    @Override
    public void apply(LivingEntity target) {
        EntityAttributeInstance instance = target.getAttributeInstance(attribute);
        if (instance != null) {
            var existing = instance.getModifier(modifier.getId());
            if (existing != null)
                instance.removeModifier(existing);
            instance.addPersistentModifier(modifier);
        }
    }
}