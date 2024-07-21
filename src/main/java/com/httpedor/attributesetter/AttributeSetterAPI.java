package com.httpedor.attributesetter;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class AttributeSetterAPI {
    static final Map<Identifier, Map<EntityAttribute, EntityAttributeModifier>> ENTITY_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EntityAttribute, Double>> BASE_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EntityAttribute, EntityAttributeModifier>> TAG_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EntityAttribute, Double>> BASE_TAG_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EquipmentSlot, Map<EntityAttribute, EntityAttributeModifier>>> ITEM_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EquipmentSlot, Map<EntityAttribute, EntityAttributeModifier>>> TAG_ITEM_MODIFIERS = new HashMap<>();

    public static void registerEntityAttributeModifier(Identifier entity, EntityAttribute attr, EntityAttributeModifier modifier) {
        System.out.println("Registering attribute modifier for " + entity + " (" + attr + ", " + modifier + ")");
        if (!ENTITY_MODIFIERS.containsKey(entity))
            ENTITY_MODIFIERS.put(entity, new HashMap<>());
        ENTITY_MODIFIERS.get(entity).put(attr, modifier);
    }
    public static void registerEntityBaseAttribute(Identifier entity, EntityAttribute attr, double baseValue) {
        System.out.println("Changing base attribute for " + entity + " (" + attr + ", " + baseValue + ")");
        if (!BASE_MODIFIERS.containsKey(entity))
            BASE_MODIFIERS.put(entity, new HashMap<>());
        BASE_MODIFIERS.get(entity).put(attr, baseValue);
    }
    public static void registerTagAttributeModifier(Identifier tag, EntityAttribute attr, EntityAttributeModifier modifier) {
        System.out.println("Registering attribute modifier for all #" + tag + " (" + attr + ", " + modifier + ")");
        if (!TAG_MODIFIERS.containsKey(tag))
            TAG_MODIFIERS.put(tag, new HashMap<>());
        TAG_MODIFIERS.get(tag).put(attr, modifier);
    }
    public static void registerTagBaseAttribute(Identifier tag, EntityAttribute attr, double baseValue) {
        System.out.println("Changing base attribute for all #" + tag + " (" + attr + ", " + baseValue + ")");
        if (!BASE_TAG_MODIFIERS.containsKey(tag))
            BASE_TAG_MODIFIERS.put(tag, new HashMap<>());
        BASE_TAG_MODIFIERS.get(tag).put(attr, baseValue);
    }
    public static void registerItemAttributeModifier(Identifier item, EntityAttribute attr, EntityAttributeModifier modifier, EquipmentSlot slot) {
        System.out.println("Registering attribute modifier for " + item + " (" + attr + ", " + modifier + ")");
        if (!ITEM_MODIFIERS.containsKey(item))
            ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!ITEM_MODIFIERS.get(item).containsKey(slot))
            ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        ITEM_MODIFIERS.get(item).get(slot).put(attr, modifier);
    }
    public static void registerTagItemAttributeModifier(Identifier tag, EntityAttribute attr, EntityAttributeModifier modifier, EquipmentSlot slot) {
        System.out.println("Registering attribute modifier for all #" + tag + " (" + attr + ", " + modifier + ")");
        if (!TAG_ITEM_MODIFIERS.containsKey(tag))
            TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, modifier);
    }
}
