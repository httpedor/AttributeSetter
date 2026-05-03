package com.httpedor.attributesetter.setters.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class ItemAttributeDependencySetter extends ItemAttributeSetter {
    public EntityAttribute attribute;
    public float multiplier = 1;
    public EntityAttribute dependency;
    public final java.util.UUID id;

    public ItemAttributeDependencySetter(EntityAttribute attribute, EntityAttribute dependency, float multiplier, String uniqueIndex) {
        this.attribute = attribute;
        this.dependency = dependency;
        this.multiplier = multiplier;
        id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void apply(ItemStack stack, EquipmentSlot slot, Multimap<EntityAttribute, EntityAttributeModifier> modifiers) {
        if (multiplier == 0)
            return;
        var existing = new ArrayList<>(modifiers.get(dependency));
        float total = 0;
        for (var mod : existing)
        {
            if (mod.getOperation() == EntityAttributeModifier.Operation.ADDITION)
                total += mod.getValue();
        }
        if (total != 0)
            modifiers.put(attribute, new EntityAttributeModifier(id, "AttributeSetter attr", total * multiplier, EntityAttributeModifier.Operation.ADDITION));
    }
}