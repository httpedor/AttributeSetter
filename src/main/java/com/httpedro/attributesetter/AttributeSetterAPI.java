package com.httpedro.attributesetter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.lang3.function.TriFunction;

import com.google.gson.JsonObject;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.entity.IdEntitySelector;
import com.httpedro.attributesetter.selectors.item.IdItemSelector;
import com.httpedro.attributesetter.setters.ASSetter;

public class AttributeSetterAPI {
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<ItemStack>>>> itemSelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<LivingEntity>>>> entitySelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<LivingEntity>, ASSetter<LivingEntity>>>> entitySetterBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<ItemStack>, ASSetter<ItemStack>>>> itemSetterBuilders = new LinkedList<>();

    static final List<Pair<ASSelector<LivingEntity>, ASSetter<LivingEntity>[]>> entitySetters = new LinkedList<>();
    // We cache ID selectors separately for performance, because they are exact matches and it'd be useless to iterate over all other selectors,
    // this is kind of a hacky way to implement it but it saves so much performance it's worth it
    static final Map<ResourceLocation, List<ASSetter<LivingEntity>>> entityIdCache = new HashMap<>();
    static final List<Pair<ASSelector<ItemStack>, ASSetter<ItemStack>[]>> itemSetters = new LinkedList<>();
    // Same as above
    static final Map<ResourceLocation, List<ASSetter<ItemStack>>> itemIdCache = new HashMap<>();

    public static ASSelector<ItemStack> parseItemSelector(String selectorString, String fileName)
    {
        int itemSelectorBuildersSize = itemSelectorBuilders.size();
        for (int i = 0; i < itemSelectorBuildersSize; i++)
        {
            try {
                var builder = itemSelectorBuilders.get(i);
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                {
                    return selector;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing item selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }
    public static ASSelector<LivingEntity> parseEntitySelector(String selectorString, String fileName)
    {
        int entitySelectorBuildersSize = entitySelectorBuilders.size();
        for (int i = 0; i < entitySelectorBuildersSize; i++)
        {
            try {
                var builder = entitySelectorBuilders.get(i);
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                {
                    return selector;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing entity selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }

    public static ASSetter<ItemStack> parseItemSetter(JsonObject obj, String id, ASSelector<ItemStack> selector)
    {
        int itemSetterBuildersSize = itemSetterBuilders.size();
        for (int i = 0; i < itemSetterBuildersSize; i++)
        {
            try {
                var builder = itemSetterBuilders.get(i);
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                {
                    return setter;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing item setter with id '{}':", id, e);
            }
        }
        return null;
    }
    public static ASSetter<LivingEntity> parseEntitySetter(JsonObject obj, String id, ASSelector<LivingEntity> selector)
    {
        int entitySetterBuildersSize = entitySetterBuilders.size();
        for (int i = 0; i < entitySetterBuildersSize; i++)
        {
            try {
                var builder = entitySetterBuilders.get(i);
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                {
                    return setter;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing entity setter with id '{}':", id, e);
            }
        }
        return null;
    }

    public static void registerItemEntry(String selectorString, Pair<JsonObject, String>[] entries, String fileName)
    {
        var selector = parseItemSelector(selectorString, fileName);
        if (selector == null)
        {
            Attributesetter.LOGGER.warn("Could not find a valid item selector for selector string '{}'", selectorString);
            return;
        }

        List<ASSetter<ItemStack>> setters = new ArrayList<>();
        for (var entry : entries)
        {
            var setter = parseItemSetter(entry.getA(), entry.getB(), selector);
            if (setter == null)
            {
                Attributesetter.LOGGER.warn("Could not find a valid item entry builder for entry '{}'", entry.getB().toString());
                continue;
            }

            setters.add(setter);
        }

        // If the selector is an ID selector, add it to the cache
        if (selector instanceof IdItemSelector idSelector)
        {
            var id = idSelector.id;
            itemIdCache.putIfAbsent(id, new ArrayList<>());
            itemIdCache.get(id).addAll(setters);
        }
        else
        {
            // Insert based on selector specificity(lower specificity at the start, higher at the end, so more specific selectors are checked last)
            // This way, more specific selectors can override less specific ones
            int index = 0;
            int itemSettersSize = itemSetters.size();
            for (int i = 0; i < itemSettersSize; i++)
            {
                var pair = itemSetters.get(i);
                if (pair.getA().getSpecificity() > selector.getSpecificity())
                {
                    break;
                }
                index++;
            }
            itemSetters.add(index, new Pair<>(selector, setters.toArray(new ASSetter[0])));
        }
    }
    public static void registerEntityEntry(String selector, Pair<JsonObject, String>[] entries, String fileName)
    {
        var asSelector = parseEntitySelector(selector, fileName);
        if (asSelector == null)
        {
            Attributesetter.LOGGER.warn("Could not find a valid entity selector for selector string '{}'", selector);
            return;
        }

        List<ASSetter<LivingEntity>> setters = new ArrayList<>();
        for (var entry : entries)
        {
            var setter = parseEntitySetter(entry.getA(), entry.getB(), asSelector);
            if (setter == null)
            {
                Attributesetter.LOGGER.warn("Could not find a valid entity entry builder for entry '{}'", entry.getB().toString());
                continue;
            }

            setters.add(setter);
        }

        // If the selector is an ID selector, add it to the cache
        if (asSelector instanceof IdEntitySelector idSelector)
        {
            var id = idSelector.id;
            entityIdCache.putIfAbsent(id, new ArrayList<>());
            entityIdCache.get(id).addAll(setters);
        }
        else
        {
            // Insert based on selector specificity(lower specificity at the start, higher at the end, so more specific selectors are checked last)
            // This way, more specific selectors can override less specific ones
            int index = 0;
            int entitySettersSize = entitySetters.size();
            for (int i = 0; i < entitySettersSize; i++)
            {
                var pair = entitySetters.get(i);
                if (pair.getA().getSpecificity() > asSelector.getSpecificity())
                {
                    break;
                }
                index++;
            }
            entitySetters.add(index, new Pair<>(asSelector, setters.toArray(new ASSetter[0])));
        }
    }

    public static List<ASSetter<ItemStack>> getEntriesFor(ItemStack stack)
    {
        List<ASSetter<ItemStack>> result = new ArrayList<>();
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemIdCache.containsKey(id))
        {
            result.addAll(itemIdCache.get(id));
        }
        int itemSettersSize = itemSetters.size();
        for (int i = 0; i < itemSettersSize; i++)
        {
            var pair = itemSetters.get(i);
            if (pair.getA().test(stack))
            {
                for (var setter : pair.getB())
                {
                    result.add(setter);
                }
            }
        }
        return result;
    }

    public static List<ASSetter<LivingEntity>> getEntriesFor(LivingEntity entity)
    {
        List<ASSetter<LivingEntity>> result = new ArrayList<>();
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityIdCache.containsKey(id))
        {
            result.addAll(entityIdCache.get(id));
        }
        int entitySettersSize = entitySetters.size();
        for (int i = 0; i < entitySettersSize; i++)
        {
            var pair = entitySetters.get(i);
            if (pair.getA().test(entity))
            {
                for (var setter : pair.getB())
                {
                    result.add(setter);
                }
            }
        }
        return result;
    }
    
    /**
     * Registers a new item selector parser
     * @param priority The priority of the parser, higher priority parsers are checked first
     * @param selector The selector parser function. The first parameter is the selector string, the second is the file name
     */
    public static void registerItemSelectorBuilder(int priority, BiFunction<String, String, ASSelector<ItemStack>> selector)
    {
        int index = 0;
        int itemSelectorBuildersSize = itemSelectorBuilders.size();
        for (int i = 0; i < itemSelectorBuildersSize; i++)
        {
            var pair = itemSelectorBuilders.get(i);
            if (priority > pair.getA())
            {
                break;
            }
            index++;
        }
        itemSelectorBuilders.add(index, new Pair<>(priority, selector));
    }
    /**
     * Registers a new entity selector parser
     * @param priority The priority of the parser, higher priority parsers are checked first
     * @param selector The selector parser function. The first parameter is the selector string, the second is the file name
     */
    public static void registerEntitySelectorBuilder(int priority, BiFunction<String, String, ASSelector<LivingEntity>> selector)
    {
        int index = 0;
        int entitySelectorBuildersSize = entitySelectorBuilders.size();
        for (int i = 0; i < entitySelectorBuildersSize; i++)
        {
            var pair = entitySelectorBuilders.get(i);
            if (priority > pair.getA())
            {
                break;
            }
            index++;
        }
        entitySelectorBuilders.add(index, new Pair<>(priority, selector));
    }

    /**
     * Registers a new entity setter builder
     * @param priority The priority of the builder, higher priority builders are checked first
     * @param builder The setter builder function. The first parameter is the JSON object, the second is the entry ID, the third is the selector. You can assume the ID is unique. The selector may be null.
     */
    public static void registerEntitySetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<LivingEntity>, ASSetter<LivingEntity>> builder)
    {
        int index = 0;
        int entitySetterBuildersSize = entitySetterBuilders.size();
        for (int i = 0; i < entitySetterBuildersSize; i++)
        {
            var pair = entitySetterBuilders.get(i);
            if (priority > pair.getA())
            {
                break;
            }
            index++;
        }
        entitySetterBuilders.add(index, new Pair<>(priority, builder));
    }

    /**
     * Registers a new item setter builder
     * @param priority The priority of the builder, higher priority builders are checked first
     * @param builder The setter builder function. The first parameter is the JSON object, the second is the entry ID, the third is the selector. You can assume the ID is unique. The selector may be null.
     */
    public static void registerItemSetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<ItemStack>, ASSetter<ItemStack>> builder)
    {
        int index = 0;
        int itemSetterBuildersSize = itemSetterBuilders.size();
        for (int i = 0; i < itemSetterBuildersSize; i++)
        {
            var pair = itemSetterBuilders.get(i);
            if (priority > pair.getA())
            {
                break;
            }
            index++;
        }
        itemSetterBuilders.add(index, new Pair<>(priority, builder));
    }
    
    public static void clearAll()
    {
        entitySetters.clear();
        entityIdCache.clear();
        itemSetters.clear();
        itemIdCache.clear();
    }
}

