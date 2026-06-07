package com.httpedro.attributesetter.setters.itemstack.attribute;

import java.util.ArrayList;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public class ItemAttributeDependencySetter extends ItemStackAttributeSetter {
    public Holder<Attribute> attribute;
    public float multiplier = 1;
    public Holder<Attribute> dependency;
    public final ResourceLocation id;
    public ItemAttributeDependencySetter(Holder<Attribute> attribute, Holder<Attribute> dependency, float multiplier, String uniqueIndex) {
        this.attribute = attribute;
        this.dependency = dependency;
        this.multiplier = multiplier;
        id = ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex);
    }

    @Override
    public void apply(ItemAttributeModifierEvent e) {
        if (multiplier == 0)
            return;
        var modifiers = new ArrayList<ItemAttributeModifiers.Entry>();
        EquipmentSlotGroup slot = null;
        for (var mod : e.getModifiers())
        {
            if (mod.attribute().equals(dependency))
            {
                modifiers.add(mod);
                if (slot == null)
                    slot = mod.slot();
            }
        }
        float total = 0;
        for (var mod : modifiers)
        {
            if (mod.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                total += mod.modifier().amount();
        }
        if (total != 0)
            e.addModifier(attribute, new AttributeModifier(id, total * multiplier, AttributeModifier.Operation.ADD_VALUE), slot);
    }
    
}

