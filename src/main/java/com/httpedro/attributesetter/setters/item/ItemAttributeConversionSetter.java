package com.httpedro.attributesetter.setters.item;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;

public class ItemAttributeConversionSetter extends ItemAttributeSetter{
    public Attribute from;
    public Attribute to;
    public float amountConverted;
    public float conversionRate;
    public final UUID id;

    public ItemAttributeConversionSetter(Attribute from, Attribute to, float amountConverted, float conversionRate, String uniqueIndex) {
        this.from = from;
        this.to = to;
        this.amountConverted = amountConverted;
        this.conversionRate = conversionRate;
        id = UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void apply(ItemAttributeModifierEvent e) {
        if (amountConverted == 0)
            return;
        var modifiers = new ArrayList<>(e.getModifiers().get(from));
        for (var mod : modifiers)
        {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION)
            {
                var convertedAmount = mod.getAmount() * amountConverted;
                var newMod = new AttributeModifier(mod.getId(), mod.getName(), mod.getAmount() - convertedAmount, AttributeModifier.Operation.ADDITION);
                e.removeModifier(from, mod);
                if (newMod.getAmount() != 0)
                    e.addModifier(from, newMod);
                if (to != null && conversionRate != 0 && convertedAmount != 0)
                    e.addModifier(to, new AttributeModifier(id, "AttributeSetter conversion", convertedAmount * conversionRate, AttributeModifier.Operation.ADDITION));
            }
        }
    }
    
}
