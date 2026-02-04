package com.httpedro.attributesetter.setters.item;

import java.nio.charset.StandardCharsets;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;

public class ItemAttributeBaseSetter extends ItemAttributeSetter{
    public Attribute attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeBaseSetter(Attribute attribute, double baseValue, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        var id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(StandardCharsets.UTF_8));
        this.modifier = new AttributeModifier(id, uniqueIndex, baseValue, AttributeModifier.Operation.ADDITION);
        this.slot = slot;
    }

    @Override
    public void apply(ItemAttributeModifierEvent e) {
        if (e.getSlotType() != slot)
            return;
        e.removeAttribute(attribute);
        e.addModifier(attribute, modifier);
    }
    
}
