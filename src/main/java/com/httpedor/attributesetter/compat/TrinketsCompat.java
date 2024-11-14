package com.httpedor.attributesetter.compat;

import com.google.gson.JsonObject;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.httpedor.attributesetter.AttributeSetter.BASE_UUID;

public class TrinketsCompat {

    static final Map<Identifier, Map<String, Map<EntityAttribute, EntityAttributeModifier>>> ITEM_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<String, Map<EntityAttribute, EntityAttributeModifier>>> TAG_ITEM_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<String, Map<EntityAttribute, Double>>> BASE_ITEM_MODIFIERS = new HashMap<>();
    static final Map<Identifier, Map<String, Map<EntityAttribute, Double>>> BASE_TAG_ITEM_MODIFIERS = new HashMap<>();

    public static boolean shouldCurioHandle(String idStr, JsonObject json)
    {
        boolean isTag = idStr.startsWith("#");
        Identifier id;
        if (isTag)
            id = new Identifier(idStr.substring(1));
        else
            id = new Identifier(idStr);


        String slot;
        boolean isCuriosSlot;
        if (json.has("slot"))
        {
            slot = json.get("slot").getAsString();
            isCuriosSlot = true;
        }
        else
        {
            return false;
            /*
            var itemEntry = ForgeRegistries.ITEMS.getValue(id);
            if (itemEntry == null)
            {
                System.out.println("Invalid item: " + id);
                return true;
            }

            var slots = CuriosApi.getCuriosHelper().getCurioTags(itemEntry);
            if (slots.isEmpty())
            {
                return false;
            }

            isCuriosSlot = true;
            slot = slots.iterator().next();
             */
        }

        if (!isCuriosSlot)
            return false;

        //Now to actually registering it
        var value = json.get("value").getAsDouble();

        var attr = Registries.ATTRIBUTE.get(new Identifier(json.get("attribute").getAsString()));
        if (attr == null)
        {
            System.out.println("Failed to find attribute " + json.get("attribute").getAsString());
            return true;
        }

        String opStr;
        if (json.has("operation"))
            opStr = json.get("operation").getAsString();
        else
            opStr = "ADDITION";
        if (opStr.equalsIgnoreCase("base"))
        {
            if (isTag)
                registerTagItemBaseAttribute(id, attr, value, slot);
            else
                registerItemBaseAttribute(id, attr, value, slot);
        }
        else
        {
            var op = EntityAttributeModifier.Operation.valueOf(opStr.toUpperCase());
            EntityAttributeModifier mod;
            if (json.has("uuid"))
                mod = new EntityAttributeModifier(UUID.fromString(json.get("uuid").getAsString()), "ASMod", value, op);
            else
                mod = new EntityAttributeModifier("ASMod", value, op);

            if (isTag)
                registerTagItemAttributeModifier(id, attr, mod, slot);
            else
                registerItemAttributeModifier(id, attr, mod, slot);
        }

        return true;
    }

    public static void registerItemAttributeModifier(Identifier item, EntityAttribute attr, EntityAttributeModifier modifier, String slot) {
        if (!ITEM_MODIFIERS.containsKey(item))
            ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!ITEM_MODIFIERS.get(item).containsKey(slot))
            ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        ITEM_MODIFIERS.get(item).get(slot).put(attr, modifier);
    }
    public static void registerTagItemAttributeModifier(Identifier tag, EntityAttribute attr, EntityAttributeModifier modifier, String slot) {
        if (!TAG_ITEM_MODIFIERS.containsKey(tag))
            TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, modifier);
    }
    public static void registerItemBaseAttribute(Identifier item, EntityAttribute attr, double baseValue, String slot) {
        if (!BASE_ITEM_MODIFIERS.containsKey(item))
            BASE_ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!BASE_ITEM_MODIFIERS.get(item).containsKey(slot))
            BASE_ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        BASE_ITEM_MODIFIERS.get(item).get(slot).put(attr, baseValue);
    }
    public static void registerTagItemBaseAttribute(Identifier tag, EntityAttribute attr, double baseValue, String slot) {
        if (!BASE_TAG_ITEM_MODIFIERS.containsKey(tag))
            BASE_TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!BASE_TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            BASE_TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        BASE_TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, baseValue);
    }
}

