package com.httpedro.attributesetter.setters.itemstack.attribute;

import java.util.ArrayList;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public class ItemAttributeConversionSetter extends ItemStackAttributeSetter {
    public Holder<Attribute> from;
    public Holder<Attribute> to;
    public float amountConverted;
    public float conversionRate;
    public final ResourceLocation id;

    public ItemAttributeConversionSetter(Holder<Attribute> from, Holder<Attribute> to, float amountConverted, float conversionRate, String uniqueIndex) {
        this.from = from;
        this.to = to;
        this.amountConverted = amountConverted;
        this.conversionRate = conversionRate;
        id = ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex);
    }

    @Override
    public void apply(ItemAttributeModifierEvent e) {
        if (amountConverted == 0)
            return;
        var modifiers = new ArrayList<ItemAttributeModifiers.Entry>();
        for (var mod : e.getModifiers())
        {
            if (mod.attribute().equals(from))
            {
                modifiers.add(mod);
            }
        }

        for (var mod : modifiers)
        {
            if (mod.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
            {
                var convertedAmount = mod.modifier().amount() * amountConverted;
                var newMod = new AttributeModifier(mod.modifier().id(), mod.modifier().amount() - convertedAmount, AttributeModifier.Operation.ADD_VALUE);
                e.removeModifier(from, mod.modifier().id());
                if (newMod.amount() != 0)
                    e.addModifier(from, newMod, mod.slot());
                if (to != null && conversionRate != 0 && convertedAmount != 0)
                    e.addModifier(to, new AttributeModifier(id, convertedAmount * conversionRate, AttributeModifier.Operation.ADD_VALUE), mod.slot());
            }
        }
    }
    
}

