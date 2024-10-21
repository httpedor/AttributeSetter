package com.httpedro.attributesetter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;

public class AttributeSetterAPI {
    static final Map<ResourceLocation, Map<Attribute, AttributeModifier>> ENTITY_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<Attribute, Double>> BASE_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<Attribute, AttributeModifier>> TAG_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<Attribute, Double>> BASE_TAG_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<EquipmentSlot, Map<Attribute, AttributeModifier>>> ITEM_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<EquipmentSlot, Map<Attribute, AttributeModifier>>> TAG_ITEM_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<EquipmentSlot, Map<Attribute, Double>>> BASE_ITEM_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<EquipmentSlot, Map<Attribute, Double>>> BASE_TAG_ITEM_MODIFIERS = new HashMap<>();

    public static void registerEntityAttributeModifier(ResourceLocation entity, Attribute attr, AttributeModifier modifier) {
        if (!ENTITY_MODIFIERS.containsKey(entity))
            ENTITY_MODIFIERS.put(entity, new HashMap<>());
        ENTITY_MODIFIERS.get(entity).put(attr, modifier);
    }
    public static void registerEntityBaseAttribute(ResourceLocation entity, Attribute attr, double baseValue) {
        if (!BASE_MODIFIERS.containsKey(entity))
            BASE_MODIFIERS.put(entity, new HashMap<>());
        BASE_MODIFIERS.get(entity).put(attr, baseValue);
    }
    public static void registerTagAttributeModifier(ResourceLocation tag, Attribute attr, AttributeModifier modifier) {
        if (!TAG_MODIFIERS.containsKey(tag))
            TAG_MODIFIERS.put(tag, new HashMap<>());
        TAG_MODIFIERS.get(tag).put(attr, modifier);
    }
    public static void registerTagBaseAttribute(ResourceLocation tag, Attribute attr, double baseValue) {
        if (!BASE_TAG_MODIFIERS.containsKey(tag))
            BASE_TAG_MODIFIERS.put(tag, new HashMap<>());
        BASE_TAG_MODIFIERS.get(tag).put(attr, baseValue);
    }
    public static void registerItemAttributeModifier(ResourceLocation item, Attribute attr, AttributeModifier modifier, EquipmentSlot slot) {
        if (!ITEM_MODIFIERS.containsKey(item))
            ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!ITEM_MODIFIERS.get(item).containsKey(slot))
            ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        ITEM_MODIFIERS.get(item).get(slot).put(attr, modifier);
    }
    public static void registerTagItemAttributeModifier(ResourceLocation tag, Attribute attr, AttributeModifier modifier, EquipmentSlot slot) {
        if (!TAG_ITEM_MODIFIERS.containsKey(tag))
            TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, modifier);
    }
    public static void registerItemBaseAttribute(ResourceLocation item, Attribute attr, double baseValue, EquipmentSlot slot) {
        if (!BASE_ITEM_MODIFIERS.containsKey(item))
            BASE_ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!BASE_ITEM_MODIFIERS.get(item).containsKey(slot))
            BASE_ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        BASE_ITEM_MODIFIERS.get(item).get(slot).put(attr, baseValue);
    }
    public static void registerTagItemBaseAttribute(ResourceLocation tag, Attribute attr, double baseValue, EquipmentSlot slot) {
        if (!BASE_TAG_ITEM_MODIFIERS.containsKey(tag))
            BASE_TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!BASE_TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            BASE_TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        BASE_TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, baseValue);
    }
}
