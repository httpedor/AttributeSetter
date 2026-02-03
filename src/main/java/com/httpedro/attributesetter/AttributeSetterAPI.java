package com.httpedro.attributesetter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Pair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttributeSetterAPI {
    private static final Map<ResourceLocation, ASSelector> selectorTypes = new HashMap<>();
    private static final Map<ResourceLocation, ASEntry> entryTypes = new HashMap<>();
    static final Map<SelectorType, LinkedList<Pair<ASSelector, ASEntry>>> ENTITY_ENTRIES = new HashMap<>();
    // We cache ID selectors separately for performance, because they are exact matches and it'd be useless to iterate over all other selectors
    static final Map<ResourceLocation, LinkedList<Pair<ASSelector, ASEntry>>> ENTITY_ID_CACHE = new HashMap<>();
    static final Map<SelectorType, LinkedList<Pair<ASSelector, ASEntry>>> ITEM_ENTRIES = new HashMap<>();
    // Same as above
    static final Map<ResourceLocation, LinkedList<Pair<ASSelector, ASEntry>>> ITEM_ID_CACHE = new HashMap<>();

    public static void registerItemEntry(ASSelector selector, ASEntry entry)
    {
        if (selector.type == SelectorType.ID)
        {
            ITEM_ID_CACHE.computeIfAbsent(selector.id, k -> new LinkedList<>());
            ITEM_ID_CACHE.get(selector.id).add(new Pair<>(selector, entry));
            return;
        }
        var entries = ITEM_ENTRIES.computeIfAbsent(selector.type, k -> new LinkedList<>());
        if (entries.isEmpty())
        {
            entries.add(new Pair<>(selector, entry));
            return;
        }
        // Insert based on priority: BASE > MODIFIER > DURABILITY
        int insertIndex = 0;
        for (var pair : entries)
        {
            var existingEntry = pair.getB();
            if (entry.type == EntryType.BASE && existingEntry.type != EntryType.BASE)
            {
                break;
            }
            else if (entry.type == EntryType.MODIFIER && existingEntry.type == EntryType.DURABILITY)
            {
                break;
            }
            insertIndex++;
        }
        entries.add(insertIndex, new Pair<>(selector, entry));
    }
    public static void registerEntityEntry(ASSelector selector, ASEntry entry)
    {
        ENTITY_ENTRIES.computeIfAbsent(selector.type, k -> new LinkedList<>());
        if (selector.type == SelectorType.ID)
        {
            ENTITY_ID_CACHE.computeIfAbsent(selector.id, k -> new LinkedList<>());
            ENTITY_ID_CACHE.get(selector.id).add(new Pair<>(selector, entry));
            return;
        }
        var entries = ENTITY_ENTRIES.get(selector.type);
        if (entries.isEmpty())
        {
            entries.add(new Pair<>(selector, entry));
            return;
        }

        if (entry.type == EntryType.BASE)
            entries.addFirst(new Pair<>(selector, entry));
        else
            entries.addLast(new Pair<>(selector, entry));
    }

    public static Collection<Pair<ASSelector, ASEntry>> possibleItemEntries(ResourceLocation id)
    {
        var ret = new ArrayList<Pair<ASSelector, ASEntry>>();
        if (ITEM_ID_CACHE.containsKey(id))
            ret.addAll(ITEM_ID_CACHE.get(id));
        for (var entryList : ITEM_ENTRIES.values())
        {
            ret.addAll(entryList);
        }
        return ret;
    }
    public static Collection<Pair<ASSelector, ASEntry>> possibleItemEntries(ResourceLocation id, SelectorType type)
    {
        if (type == SelectorType.ID)
        {
            if (ITEM_ID_CACHE.containsKey(id))
                return ITEM_ID_CACHE.get(id);
            return new ArrayList<>();
        }
        if (!ITEM_ENTRIES.containsKey(type)) return new ArrayList<>();
        return ITEM_ENTRIES.get(type);
    }
    public static Collection<Pair<ASSelector, ASEntry>> possibleEntityEntries(ResourceLocation id)
    {
        var ret = new ArrayList<Pair<ASSelector, ASEntry>>();
        if (ENTITY_ID_CACHE.containsKey(id))
            ret.addAll(ENTITY_ID_CACHE.get(id));
        for (var entryList : ENTITY_ENTRIES.values())
        {
            ret.addAll(entryList);
        }
        return ret;
    }
    public static Collection<Pair<ASSelector, ASEntry>> possibleEntityEntries(ResourceLocation id, SelectorType type)
    {
        if (type == SelectorType.ID)
        {
            if (ENTITY_ID_CACHE.containsKey(id))
                return ENTITY_ID_CACHE.get(id);
            else
                return new ArrayList<>();
        }
        if (!ENTITY_ENTRIES.containsKey(type)) return new ArrayList<>();
        return ENTITY_ENTRIES.get(type);
    }

    public static Collection<ASEntry> getEntriesFor(ItemStack stack, EquipmentSlot slot)
    {
        List<ASEntry> results = new ArrayList<>();

        var item = stack.getItem();
        var selectorOrder = List.of(SelectorType.TAG, SelectorType.NBT, SelectorType.ID);

        for (var type : selectorOrder)
        {
            for (var entry : AttributeSetterAPI.possibleItemEntries(ForgeRegistries.ITEMS.getKey(item), type))
            {
                var selector = entry.getA();
                var ase = entry.getB();
                if (selector.test(stack) && ase.slot == slot)
                {
                    results.add(ase);
                }
            }
        }

        return results;
    }

    public static Collection<ASEntry> getEntriesFor(LivingEntity entity)
    {
        List<ASEntry> results = new ArrayList<>();

        var entityType = entity.getType();
        var id = EntityType.getKey(entityType);
        var selectorOrder = List.of(SelectorType.TAG, SelectorType.NBT, SelectorType.ID);

        for (var type : selectorOrder)
        {
            for (var entry : AttributeSetterAPI.possibleEntityEntries(id, type))
            {
                var selector = entry.getA();
                var ase = entry.getB();
                if (selector.test(entity))
                {
                    results.add(ase);
                }
            }
        }

        return results;
    }
    
    public static void registerSelector(ResourceLocation id, ASSelector selector)
    {
        selectorTypes.put(id, selector);
    }
    public static void registerEntry(ResourceLocation id, ASEntry entry)
    {
        entryTypes.put(id, entry);
    }
    
    public static void clearAll()
    {
        ENTITY_ENTRIES.clear();
        ENTITY_ID_CACHE.clear();
        ITEM_ENTRIES.clear();
        ITEM_ID_CACHE.clear();
    }
    
    public static UUID generateDeterministicUUID(String modSource, String identifier, String attribute, String slot) {
        String combined = modSource + ":" + identifier + ":" + attribute + ":" + slot + ":";
        return UUID.nameUUIDFromBytes(combined.getBytes(StandardCharsets.UTF_8));
    }
}
