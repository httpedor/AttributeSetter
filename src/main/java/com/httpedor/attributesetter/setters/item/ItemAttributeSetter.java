package com.httpedor.attributesetter.setters.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;

public abstract class ItemAttributeSetter extends ItemSetter {
    @Override
    public void apply(ItemStack target) {
        throw new UnsupportedOperationException("Use the slot-aware apply method");
    }

    public abstract void apply(ItemStack stack, EquipmentSlot slot, Multimap<EntityAttribute, EntityAttributeModifier> modifiers);
}