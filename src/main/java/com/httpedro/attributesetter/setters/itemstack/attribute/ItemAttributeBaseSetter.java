package com.httpedro.attributesetter.setters.itemstack.attribute;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public class ItemAttributeBaseSetter extends ItemStackAttributeSetter {
    public Holder<Attribute> attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;

    public ItemAttributeBaseSetter(Holder<Attribute> attribute, double baseValue, EquipmentSlot slot, String uniqueIndex) {
        this.attribute = attribute;
        // NeoForge only renders the green "base" line for the modifier whose id matches the attribute's base id
        // (minecraft:base_attack_damage / base_attack_speed / base_entity_reach). Registering the base value under
        // our own id makes it render as a separate blue "+N" line instead of replacing the item's base value.
        ResourceLocation baseId = attribute.value().getBaseId();
        ResourceLocation id = baseId != null ? baseId : ResourceLocation.fromNamespaceAndPath("attributesetter", uniqueIndex);
        this.modifier = new AttributeModifier(id, baseValue, AttributeModifier.Operation.ADD_VALUE);
        this.slot = slot;
    }

    @Override
    protected void applyModifiers(ItemAttributeModifierEvent e) {
        e.removeAllModifiersFor(attribute);
        e.addModifier(attribute, modifier, EquipmentSlotGroup.bySlot(slot));
    }
    
}
