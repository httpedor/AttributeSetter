package com.httpedor.attributesetter.setters.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;

public class ItemAttributeBaseSetter extends ItemAttributeSetter {
    public EntityAttribute attribute;
    public EntityAttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeBaseSetter(EntityAttribute attribute, double baseValue, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        var id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.modifier = new EntityAttributeModifier(id, uniqueIndex, baseValue, EntityAttributeModifier.Operation.ADDITION);
        this.slot = slot;
    }

    @Override
    public void apply(ItemStack stack, EquipmentSlot slot, Multimap<EntityAttribute, EntityAttributeModifier> modifiers) {
        if (slot != this.slot)
            return;
        modifiers.removeAll(attribute);
        modifiers.put(attribute, modifier);
    }
}