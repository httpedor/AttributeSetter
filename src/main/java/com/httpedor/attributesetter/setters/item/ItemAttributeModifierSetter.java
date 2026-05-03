package com.httpedor.attributesetter.setters.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;

public class ItemAttributeModifierSetter extends ItemAttributeSetter {
    public EntityAttribute attribute;
    public EntityAttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeModifierSetter(EntityAttribute attribute, EntityAttributeModifier.Operation op, double amount, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        this.slot = slot;
        var id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.modifier = new EntityAttributeModifier(id, uniqueIndex, amount, op);
    }

    @Override
    public void apply(ItemStack stack, EquipmentSlot slot, Multimap<EntityAttribute, EntityAttributeModifier> modifiers) {
        if (slot == this.slot)
        {
            modifiers.remove(attribute, modifier);
            modifiers.put(attribute, modifier);
        }
    }
}