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
    public Attribute attribute;
    public AttributeModifier modifier;
    public EquipmentSlot slot;
    public double value;

    protected ASEntry() {}

    protected static ASEntry forEntity(ResourceLocation modSource, EntryType type, String id, Attribute attribute, AttributeModifier.Operation op, double value)
    {
        var entry = new ASEntry();
        entry.type = type;
        entry.attribute = attribute;
        if (type == EntryType.MODIFIER) {
            UUID uuid = AttributeSetterAPI.generateDeterministicUUID(modSource.toString(), id, ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(), "entity");
            entry.modifier = new AttributeModifier(uuid, "AttributeSetter Modifier", value, op);
        } else if (type == EntryType.BASE) {
            entry.value = value;
        }
        return entry;
    }
    public static ASEntry forEntityBase(ResourceLocation modSource, String id, Attribute attribute, double value)
    {
        return forEntity(modSource, EntryType.BASE, id, attribute, null, value);
    }
    public static ASEntry forEntityModifier(ResourceLocation modSource, String id, Attribute attribute, AttributeModifier.Operation op, double value)
    {
        return forEntity(modSource, EntryType.MODIFIER, id, attribute, op, value);
    }

    public static ASEntry forItem(String modSource, EntryType type, String id, Attribute attribute, EquipmentSlot slot, AttributeModifier.Operation op, double value)
    {
        var entry = new ASEntry();
        entry.type = type;
        entry.attribute = attribute;
        entry.slot = slot;
        entry.value = value;

        if (attribute != null && slot != null && id != null)
        {
            UUID uuid = AttributeSetterAPI.generateDeterministicUUID(modSource, id, ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(), slot.getName());
            entry.modifier = new AttributeModifier(uuid, id, value, type == EntryType.BASE ? AttributeModifier.Operation.ADDITION : op);
        }
        return entry;
    }

    public void applyToEntity(LivingEntity e)
    {
        switch (type) {
            case MODIFIER:
                var attr = e.getAttribute(attribute);
                if (attr != null)
                {
                    if (attr.getModifier(modifier.getId()) != null)
                    {
                        attr.removeModifier(modifier.getId());
                        Attributesetter.LOGGER.warn("AttributeSetter: Duplicate modifier UUID {} for entity {}, replacing existing modifier. Name: {}", modifier.getId(), e.getStringUUID(), modifier.getName());
                    }
                    attr.addPermanentModifier(modifier);
                }
                break;
            case BASE:
                e.getAttribute(attribute).setBaseValue(value);
                break;
            default:
                break;
        }
    }
    public void applyToItem(ItemAttributeModifierEvent e)
    {
        switch (type) {
            case MODIFIER:
                if (!e.addModifier(attribute, modifier))
                {
                    Attributesetter.LOGGER.warn("AttributeSetter: Duplicate modifier UUID {} for item {}, skipping modifier. Name: {}", modifier.getId(), e.getItemStack().getItem().getName(e.getItemStack()), modifier.getName());
                }
                break;
            case BASE:
                e.removeAttribute(attribute);
                e.addModifier(attribute, modifier);
                break;
            case DURABILITY:
                //TODO: Change maxDurability of the item
                e.getItemStack().getItem().maxDamage = (int)value;
                break;
            default:
                break;
        }
    }

}
