package com.httpedro.attributesetter;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class ASEntry {
    public EntryType type;
    public ResourceLocation id;
    public Attribute attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;
    public double baseValue;

    protected ASEntry() {}

    protected static ASEntry forEntity(ResourceLocation modSource, EntryType type, ResourceLocation id, Attribute attribute, AttributeModifier.Operation op, double value)
    {
        var entry = new ASEntry();
        entry.type = type;
        entry.id = id;
        entry.attribute = attribute;
        if (type == EntryType.MODIFIER) {
            UUID uuid = AttributeSetterAPI.generateDeterministicUUID(modSource.toString(), id.toString(), ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(), "entity");
            entry.modifier = new AttributeModifier(uuid, "AttributeSetter Modifier", value, op);
        } else if (type == EntryType.BASE) {
            entry.baseValue = value;
        }
        return entry;
    }
    public static ASEntry forEntityBase(ResourceLocation modSource, ResourceLocation id, Attribute attribute, double value)
    {
        return forEntity(modSource, EntryType.BASE, id, attribute, null, value);
    }
    public static ASEntry forEntityModifier(ResourceLocation modSource, ResourceLocation id, Attribute attribute, AttributeModifier.Operation op, double value)
    {
        return forEntity(modSource, EntryType.MODIFIER, id, attribute, op, value);
    }

    public static ASEntry forItem(ResourceLocation modSource, EntryType type, ResourceLocation id, Attribute attribute, EquipmentSlot slot, AttributeModifier.Operation op, double value)
    {
        var entry = new ASEntry();
        entry.type = type;
        entry.id = id;
        entry.attribute = attribute;
        entry.slot = slot;
        if (type == EntryType.MODIFIER || type == EntryType.BASE) {
            UUID uuid = AttributeSetterAPI.generateDeterministicUUID(modSource.toString(), id.toString(), ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(), slot.getName());
            entry.modifier = new AttributeModifier(uuid, "AttributeSetter Modifier", value, op);
        } else if (type == EntryType.DURABILITY) {
            entry.baseValue = value;
        }
        return entry;
    }

    public void applyToEntity(LivingEntity e)
    {
        switch (type) {
            case MODIFIER:
                var attr = e.getAttribute(attribute);
                if (attr != null)
                    attr.addPermanentModifier(modifier);
                break;
            case BASE:
                e.getAttribute(attribute).setBaseValue(baseValue);
                break;
            default:
                break;
        }
    }
    public void applyToItem(ItemAttributeModifierEvent e)
    {
        switch (type) {
            case MODIFIER:
                e.addModifier(attribute, modifier);
                break;
            case BASE:
                e.removeAttribute(attribute);
                e.addModifier(attribute, modifier);
                break;
            case DURABILITY:
                //TODO: Change maxDurability of the item
                break;
            default:
                break;
        }
    }

}
