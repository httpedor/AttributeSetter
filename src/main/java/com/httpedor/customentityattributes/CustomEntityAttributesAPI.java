package com.httpedor.customentityattributes;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class CustomEntityAttributesAPI {
    static final Map<Identifier, Map<EntityAttribute, EntityAttributeModifier>> ENTITY_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EntityAttribute, Double>> BASE_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EntityAttribute, EntityAttributeModifier>> TAG_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<EntityAttribute, Double>> BASE_TAG_MODIFIERS = new HashMap<>();

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
}
