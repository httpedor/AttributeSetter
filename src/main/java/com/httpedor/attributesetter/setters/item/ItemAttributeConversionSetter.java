package com.httpedor.attributesetter.setters.item;

import com.google.common.collect.Multimap;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class ItemAttributeConversionSetter extends ItemAttributeSetter {
    public EntityAttribute from;
    public EntityAttribute to;
    public float amountConverted;
    public float conversionRate;
    public final java.util.UUID id;

    public ItemAttributeConversionSetter(EntityAttribute from, EntityAttribute to, float amountConverted, float conversionRate, String uniqueIndex) {
        this.from = from;
        this.to = to;
        this.amountConverted = amountConverted;
        this.conversionRate = conversionRate;
        id = java.util.UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void apply(ItemStack stack, EquipmentSlot slot, Multimap<EntityAttribute, EntityAttributeModifier> modifiers) {
        if (amountConverted == 0)
            return;
        var existing = new ArrayList<>(modifiers.get(from));
        for (var mod : existing)
        {
            if (mod.getOperation() == EntityAttributeModifier.Operation.ADDITION)
            {
                var convertedAmount = mod.getValue() * amountConverted;
                var newMod = new EntityAttributeModifier(mod.getId(), mod.getName(), mod.getValue() - convertedAmount, EntityAttributeModifier.Operation.ADDITION);
                modifiers.remove(from, mod);
                if (newMod.getValue() != 0)
                    modifiers.put(from, newMod);
                if (to != null && conversionRate != 0 && convertedAmount != 0)
                    modifiers.put(to, new EntityAttributeModifier(id, "AttributeSetter conversion", convertedAmount * conversionRate, EntityAttributeModifier.Operation.ADDITION));
            }
        }
    }
}