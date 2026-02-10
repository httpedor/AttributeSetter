package com.httpedro.attributesetter.setters.item;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;

public class ItemAttributeDependencySetter extends ItemAttributeSetter{
    public Attribute attribute;
    public float multiplier = 1;
    public Attribute dependency;
    public final UUID id;
    public ItemAttributeDependencySetter(Attribute attribute, Attribute dependency, float multiplier, String uniqueIndex) {
        this.attribute = attribute;
        this.dependency = dependency;
        this.multiplier = multiplier;
        id = UUID.nameUUIDFromBytes(uniqueIndex.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Override
    public void apply(ItemAttributeModifierEvent e) {
        if (multiplier == 0)
            return;
        var modifiers = new ArrayList<>(e.getModifiers().get(dependency));
        float total = 0;
        for (var mod : modifiers)
        {
            if (mod.getOperation() == AttributeModifier.Operation.ADDITION)
                total += mod.getAmount();
        }
        if (total != 0)
            e.addModifier(attribute, new AttributeModifier(id, "AttributeSetter attr", total * multiplier, AttributeModifier.Operation.ADDITION));
    }
    
}
