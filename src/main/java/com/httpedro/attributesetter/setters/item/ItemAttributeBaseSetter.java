package com.httpedro.attributesetter.setters.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public class ItemAttributeBaseSetter extends ItemAttributeSetter{
    public Holder<Attribute> attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeBaseSetter(Holder<Attribute> attribute, double baseValue, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        this.modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex), baseValue, AttributeModifier.Operation.ADD_VALUE);
        this.slot = slot;
    }

    @Override
    public void apply(ItemAttributeModifierEvent e) {
        e.removeAllModifiersFor(attribute);
        e.addModifier(attribute, modifier, EquipmentSlotGroup.bySlot(slot));
    }
    
}

