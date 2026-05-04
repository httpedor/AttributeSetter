package com.httpedro.attributesetter.compat;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public class CurioItemModifierSetter extends CurioItemSetter{
    public Holder<Attribute> attribute;
    public AttributeModifier modifier;
    public String slot;

    public CurioItemModifierSetter(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, String slot, String uniqueIndex) {
        this.modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex), amount, operation);
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

