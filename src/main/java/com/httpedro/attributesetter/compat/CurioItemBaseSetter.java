package com.httpedro.attributesetter.compat;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public class CurioItemBaseSetter extends CurioItemSetter {
    
    public Attribute attribute;
    public AttributeModifier modifier;
    public String slot;

    public CurioItemBaseSetter(Attribute attribute, double amount, String slot, String uniqueIndex) {
        var id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.modifier = new AttributeModifier(id, uniqueIndex, amount, AttributeModifier.Operation.ADDITION);
        this.attribute = attribute;
    }
    @Override
    public void apply(CurioAttributeModifierEvent e) {
        if (e.getSlotContext().identifier().equals(this.slot))
        {
            e.removeAttribute(attribute);
            e.addModifier(attribute, modifier);
        }
    }
    
}
