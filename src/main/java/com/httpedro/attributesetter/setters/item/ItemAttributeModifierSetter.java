package com.httpedro.attributesetter.setters.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public class ItemAttributeModifierSetter extends ItemAttributeSetter {
    public Holder<Attribute> attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeModifierSetter(Holder<Attribute> attribute, AttributeModifier.Operation op, double amount, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        this.slot = slot;
        this.modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex), amount, op);
    }
    
    @Override
    public void apply(ItemAttributeModifierEvent e)
    {
        e.removeModifier(attribute, modifier.id());
        e.addModifier(attribute, modifier, EquipmentSlotGroup.bySlot(slot));
    }
}

