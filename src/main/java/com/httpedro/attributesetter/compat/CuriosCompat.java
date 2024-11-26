package com.httpedro.attributesetter.compat;

import com.google.gson.JsonObject;
import com.httpedro.attributesetter.AttributeSetterAPI;
import com.httpedro.attributesetter.Attributesetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.httpedro.attributesetter.Attributesetter.BASE_UUID;

public class CuriosCompat {

    static final Map<ResourceLocation, Map<String, Map<Attribute, AttributeModifier>>> ITEM_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<String, Map<Attribute, AttributeModifier>>> TAG_ITEM_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<String, Map<Attribute, Double>>> BASE_ITEM_MODIFIERS = new HashMap<>();
    static final Map<ResourceLocation, Map<String, Map<Attribute, Double>>> BASE_TAG_ITEM_MODIFIERS = new HashMap<>();

    public static boolean shouldCurioHandle(String idStr, JsonObject json)
    {
        boolean isTag = idStr.startsWith("#");
        ResourceLocation id;
        if (isTag)
            id = new ResourceLocation(idStr.substring(1));
        else
            id = new ResourceLocation(idStr);


        String slot;
        if (json.has("slot"))
        {
            slot = json.get("slot").getAsString();
            if (!slot.startsWith("curio:"))
                return false;
            else
                slot = slot.substring(slot.indexOf(':')+1);
        }
        else
        {
            return false;
        }

        //Now to actually registering it
        var value = json.get("value").getAsDouble();

        var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(json.get("attribute").getAsString()));
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
            var op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
            AttributeModifier mod;
            if (json.has("uuid"))
                mod = new AttributeModifier(UUID.fromString(json.get("uuid").getAsString()), "ASMod", value, op);
            else
                mod = new AttributeModifier("ASMod", value, op);

            if (isTag)
                registerTagItemAttributeModifier(id, attr, mod, slot);
            else
                registerItemAttributeModifier(id, attr, mod, slot);
        }

        return true;
    }

    @SubscribeEvent
    public void curioAttributeModifier(CurioAttributeModifierEvent e)
    {
        var stack = e.getItemStack();
        var slot = e.getSlotContext().identifier();

        var item = stack.getItem();
        var id = ForgeRegistries.ITEMS.getKey(item);
        for (var entry : BASE_TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.is(TagKey.create(Registries.ITEM, entry.getKey()))
                    && entry.getValue().containsKey(slot))
            {
                for (var modEntry : entry.getValue().get(slot).entrySet())
                {
                    e.removeAttribute(modEntry.getKey());
                    e.addModifier(modEntry.getKey(), new AttributeModifier(BASE_UUID, "ASMod", modEntry.getValue(), AttributeModifier.Operation.ADDITION));
                }
            }
        }
        for (var entry : BASE_ITEM_MODIFIERS.entrySet())
        {
            if (entry.getKey().equals(id) && entry.getValue().containsKey(slot))
            {
                for (var modEntry : entry.getValue().get(slot).entrySet())
                {
                    e.removeAttribute(modEntry.getKey());
                    e.addModifier(modEntry.getKey(), new AttributeModifier(BASE_UUID, "ASMod", modEntry.getValue(), AttributeModifier.Operation.ADDITION));
                }
            }
        }
        for (var entry : TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.is(TagKey.create(Registries.ITEM, entry.getKey())) && entry.getValue().containsKey(slot))
            {
                for (var modEntry : entry.getValue().get(slot).entrySet())
                {
                    e.addModifier(modEntry.getKey(), modEntry.getValue());
                }
            }
        }

        var modifiers = ITEM_MODIFIERS.getOrDefault(id, null);
        if (modifiers != null)
        {
            var slotMods = modifiers.getOrDefault(slot, null);
            if (slotMods != null)
            {
                for (var entry : slotMods.entrySet())
                {
                    e.addModifier(entry.getKey(), entry.getValue());
                }
            }
        }

    }

    public static void registerItemAttributeModifier(ResourceLocation item, Attribute attr, AttributeModifier modifier, String slot) {
        if (!ITEM_MODIFIERS.containsKey(item))
            ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!ITEM_MODIFIERS.get(item).containsKey(slot))
            ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        ITEM_MODIFIERS.get(item).get(slot).put(attr, modifier);
    }
    public static void registerTagItemAttributeModifier(ResourceLocation tag, Attribute attr, AttributeModifier modifier, String slot) {
        if (!TAG_ITEM_MODIFIERS.containsKey(tag))
            TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, modifier);
    }
    public static void registerItemBaseAttribute(ResourceLocation item, Attribute attr, double baseValue, String slot) {
        if (!BASE_ITEM_MODIFIERS.containsKey(item))
            BASE_ITEM_MODIFIERS.put(item, new HashMap<>());
        if (!BASE_ITEM_MODIFIERS.get(item).containsKey(slot))
            BASE_ITEM_MODIFIERS.get(item).put(slot, new HashMap<>());

        BASE_ITEM_MODIFIERS.get(item).get(slot).put(attr, baseValue);
    }
    public static void registerTagItemBaseAttribute(ResourceLocation tag, Attribute attr, double baseValue, String slot) {
        if (!BASE_TAG_ITEM_MODIFIERS.containsKey(tag))
            BASE_TAG_ITEM_MODIFIERS.put(tag, new HashMap<>());
        if (!BASE_TAG_ITEM_MODIFIERS.get(tag).containsKey(slot))
            BASE_TAG_ITEM_MODIFIERS.get(tag).put(slot, new HashMap<>());

        BASE_TAG_ITEM_MODIFIERS.get(tag).get(slot).put(attr, baseValue);
    }
}
