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
import java.util.stream.Collectors;

public class AttributeSetterAPI {
    static final Map<ResourceLocation, Map<SelectorType, LinkedList<Pair<ASSelector, ASEntry>>>> ENTITY_ENTRIES = new HashMap<>();
    static final Map<ResourceLocation, Map<SelectorType, LinkedList<Pair<ASSelector, ASEntry>>>> ITEM_ENTRIES = new HashMap<>();

    public static void registerItemEntry(ResourceLocation id, ASSelector selector, ASEntry entry)
    {
        var entries = ITEM_ENTRIES.computeIfAbsent(id, k -> new HashMap<>()).computeIfAbsent(selector.type, k -> new LinkedList<>());
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
    public static void registerEntityEntry(ResourceLocation id, ASSelector selector, ASEntry entry)
    {
        ENTITY_ENTRIES.computeIfAbsent(id, k -> new HashMap<>()).computeIfAbsent(selector.type, k -> new LinkedList<>());
        var entries = ENTITY_ENTRIES.get(id).get(selector.type);
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

    public static Collection<Pair<ASSelector, ASEntry>> getItemEntries(ResourceLocation id)
    {
        if (!ITEM_ENTRIES.containsKey(id)) return new ArrayList<>();
        return ITEM_ENTRIES.get(id).values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
    public static Collection<Pair<ASSelector, ASEntry>> getItemEntries(ResourceLocation id, SelectorType type)
    {
        if (!ITEM_ENTRIES.containsKey(id)) return new ArrayList<>();
        var map = ITEM_ENTRIES.get(id);
        if (!map.containsKey(type)) return new ArrayList<>();
        return map.get(type);
    }
    public static Collection<Pair<ASSelector, ASEntry>> getEntityEntries(ResourceLocation id)
    {
        if (!ENTITY_ENTRIES.containsKey(id)) return new ArrayList<>();
        return ENTITY_ENTRIES.get(id).values().stream().flatMap(List::stream).collect(Collectors.toList());
    }
    public static Collection<Pair<ASSelector, ASEntry>> getEntityEntries(ResourceLocation id, SelectorType type)
    {
        if (!ENTITY_ENTRIES.containsKey(id)) return new ArrayList<>();
        var map = ENTITY_ENTRIES.get(id);
        if (!map.containsKey(type)) return new ArrayList<>();
        return map.get(type);
    }

    public static Collection<ASEntry> getEntriesFor(ItemStack stack, EquipmentSlot slot)
    {
        List<ASEntry> results = new ArrayList<>();

        var item = stack.getItem();
        var id = ForgeRegistries.ITEMS.getKey(item);
        var selectorOrder = List.of(SelectorType.TAG, SelectorType.NBT, SelectorType.ID);

        for (var type : selectorOrder)
        {
            for (var entry : AttributeSetterAPI.getItemEntries(id, type))
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
            for (var entry : AttributeSetterAPI.getEntityEntries(id, type))
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
    
    public static void clearAll()
    {
        ENTITY_ENTRIES.clear();
        ITEM_ENTRIES.clear();
    }
    
    public static UUID generateDeterministicUUID(String modSource, String identifier, String attribute, String slot) {
        String combined = modSource + ":" + identifier + ":" + attribute + ":" + slot + ":";
        return UUID.nameUUIDFromBytes(combined.getBytes(StandardCharsets.UTF_8));
    }
}
