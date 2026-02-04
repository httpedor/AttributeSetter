package com.httpedro.attributesetter.setters.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;

public class ItemAttributeModifierSetter extends ItemAttributeSetter {
    public Attribute attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeModifierSetter(Attribute attribute, AttributeModifier.Operation op, double amount, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        this.slot = slot;
        var id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.modifier = new AttributeModifier(id, uniqueIndex, amount, op);
    }
    
    @Override
    public void apply(ItemAttributeModifierEvent e)
    {
        if (e.getSlotType() == this.slot)
        {
            e.removeModifier(attribute, modifier);
            e.addModifier(attribute, modifier);
        }
    }
}
