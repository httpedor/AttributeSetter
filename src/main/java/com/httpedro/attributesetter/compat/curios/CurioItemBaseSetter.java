package com.httpedro.attributesetter.compat.curios;

import com.httpedro.attributesetter.compat.curios.CurioItemSetter;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

public class CurioItemBaseSetter extends CurioItemSetter {

    public Holder<Attribute> attribute;
    public AttributeModifier modifier;
    public String slot;

    public CurioItemBaseSetter(Holder<Attribute> attribute, double amount, String slot, String uniqueIndex) {
        this.modifier = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex), amount, AttributeModifier.Operation.ADD_VALUE);
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

    @Override
    public ItemStack getTarget(CurioAttributeModifierEvent event) {
        return event.getItemStack();
    }
}
