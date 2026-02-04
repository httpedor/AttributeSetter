package com.httpedro.attributesetter.compat;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public class CurioItemModifierSetter extends CurioItemSetter{
    public Attribute attribute;
    public AttributeModifier modifier;
    public String slot;

    public CurioItemModifierSetter(Attribute attribute, double amount, AttributeModifier.Operation operation, String slot, String uniqueIndex) {
        var id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.modifier = new AttributeModifier(id, uniqueIndex, amount, operation);
        this.slot = slot;
        this.attribute = attribute;
    }

    @Override
    public void apply(CurioAttributeModifierEvent e) {
        if (e.getSlotContext().identifier().equals(this.slot))
        {
            e.removeModifier(attribute, modifier);
            e.addModifier(attribute, modifier);
        }
    }
    
}
