package com.httpedor.attributesetter;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.httpedor.attributesetter.selectors.ASSelector;
import com.httpedor.attributesetter.selectors.CompositeASSelector;
import com.httpedor.attributesetter.selectors.entity.IdEntitySelector;
import com.httpedor.attributesetter.selectors.item.IdItemSelector;
import com.httpedor.attributesetter.setters.ASSetter;
import com.httpedor.attributesetter.setters.entity.EntitySetter;
import com.httpedor.attributesetter.setters.item.ItemAttributeSetter;
import com.httpedor.attributesetter.setters.item.ItemSetter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class AttributeSetterAPI {
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<ItemStack>>>> itemSelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<LivingEntity>>>> entitySelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<ItemStack>, ASSetter<ItemStack>>>> itemSetterBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<LivingEntity>, ASSetter<LivingEntity>>>> entitySetterBuilders = new LinkedList<>();

    private static final List<Pair<ASSelector<LivingEntity>, ASSetter<LivingEntity>[]>> entitySetters = new LinkedList<>();
    private static final Map<Identifier, List<ASSetter<LivingEntity>>> entityIdCache = new HashMap<>();
    private static final List<Pair<ASSelector<ItemStack>, ASSetter<ItemStack>[]>> itemSetters = new LinkedList<>();
    private static final Map<Identifier, List<ASSetter<ItemStack>>> itemIdCache = new HashMap<>();

    public static ASSelector<ItemStack> parseItemSelector(String selectorString, String fileName)
    {
        int size = itemSelectorBuilders.size();
        for (int i = 0; i < size; i++)
        {
            try
            {
                var builder = itemSelectorBuilders.get(i);
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                    return selector;
            }
            catch (Exception e)
            {
                AttributeSetter.LOGGER.error("Error while parsing item selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }

    public static ASSelector<LivingEntity> parseEntitySelector(String selectorString, String fileName)
    {
        int size = entitySelectorBuilders.size();
        for (int i = 0; i < size; i++)
        {
            try
            {
                var builder = entitySelectorBuilders.get(i);
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                    return selector;
            }
            catch (Exception e)
            {
                AttributeSetter.LOGGER.error("Error while parsing entity selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }

    public static ASSetter<ItemStack> parseItemSetter(JsonObject obj, String id, ASSelector<ItemStack> selector)
    {
        int size = itemSetterBuilders.size();
        for (int i = 0; i < size; i++)
        {
            try
            {
                var builder = itemSetterBuilders.get(i);
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                    return setter;
            }
            catch (Exception e)
            {
                AttributeSetter.LOGGER.error("Error while parsing item setter with id '{}':", id, e);
            }
        }
        return null;
    }

    public static ASSetter<LivingEntity> parseEntitySetter(JsonObject obj, String id, ASSelector<LivingEntity> selector)
    {
        int size = entitySetterBuilders.size();
        for (int i = 0; i < size; i++)
        {
            try
            {
                var builder = entitySetterBuilders.get(i);
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                    return setter;
            }
            catch (Exception e)
            {
                AttributeSetter.LOGGER.error("Error while parsing entity setter with id '{}':", id, e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void registerItemEntry(String selectorString, Pair<JsonObject, String>[] entries, String fileName)
    {
        var selector = parseItemSelector(selectorString, fileName);
        if (selector == null)
        {
            AttributeSetter.LOGGER.warn("Could not find a valid item selector for selector string '{}'", selectorString);
            return;
        }

        List<ASSetter<ItemStack>> setters = new ArrayList<>();
        for (var entry : entries)
        {
            var setter = parseItemSetter(entry.getA(), entry.getB(), selector);
            if (setter == null)
            {
                AttributeSetter.LOGGER.warn("Could not find a valid item entry builder for entry '{}'", entry.getB());
                continue;
            }
            setters.add(setter);
        }

        if (selector instanceof IdItemSelector idSelector)
        {
            itemIdCache.putIfAbsent(idSelector.id, new ArrayList<>());
            itemIdCache.get(idSelector.id).addAll(setters);
            return;
        }

        int index = 0;
        int size = itemSetters.size();
        for (int i = 0; i < size; i++)
        {
            var pair = itemSetters.get(i);
            if (pair.getA().getSpecificity() > selector.getSpecificity())
                break;
            index++;
        }
        itemSetters.add(index, new Pair<>(selector, setters.toArray(new ASSetter[0])));
    }

    @SuppressWarnings("unchecked")
    public static void registerEntityEntry(String selectorString, Pair<JsonObject, String>[] entries, String fileName)
    {
        var selector = parseEntitySelector(selectorString, fileName);
        if (selector == null)
        {
            AttributeSetter.LOGGER.warn("Could not find a valid entity selector for selector string '{}'", selectorString);
            return;
        }

        List<ASSetter<LivingEntity>> setters = new ArrayList<>();
        for (var entry : entries)
        {
            var setter = parseEntitySetter(entry.getA(), entry.getB(), selector);
            if (setter == null)
            {
                AttributeSetter.LOGGER.warn("Could not find a valid entity entry builder for entry '{}'", entry.getB());
                continue;
            }
            setters.add(setter);
        }

        if (selector instanceof IdEntitySelector idSelector)
        {
            entityIdCache.putIfAbsent(idSelector.id, new ArrayList<>());
            entityIdCache.get(idSelector.id).addAll(setters);
            return;
        }

        int index = 0;
        int size = entitySetters.size();
        for (int i = 0; i < size; i++)
        {
            var pair = entitySetters.get(i);
            if (pair.getA().getSpecificity() > selector.getSpecificity())
                break;
            index++;
        }
        entitySetters.add(index, new Pair<>(selector, setters.toArray(new ASSetter[0])));
    }

    public static List<ASSetter<ItemStack>> getEntriesFor(ItemStack stack)
    {
        List<ASSetter<ItemStack>> result = new ArrayList<>();
        var id = net.minecraft.registry.Registries.ITEM.getId(stack.getItem());
        if (itemIdCache.containsKey(id))
            result.addAll(itemIdCache.get(id));
        for (var pair : itemSetters)
        {
            if (pair.getA().test(stack))
            {
                for (var setter : pair.getB())
                    result.add(setter);
            }
        }
        return result;
    }

    public static List<ASSetter<LivingEntity>> getEntriesFor(LivingEntity entity)
    {
        List<ASSetter<LivingEntity>> result = new ArrayList<>();
        var id = net.minecraft.registry.Registries.ENTITY_TYPE.getId(entity.getType());
        if (entityIdCache.containsKey(id))
            result.addAll(entityIdCache.get(id));
        for (var pair : entitySetters)
        {
            if (pair.getA().test(entity))
            {
                for (var setter : pair.getB())
                    result.add(setter);
            }
        }
        return result;
    }

    public static void registerItemSelectorBuilder(int priority, BiFunction<String, String, ASSelector<ItemStack>> selector)
    {
        int index = 0;
        int size = itemSelectorBuilders.size();
        for (int i = 0; i < size; i++)
        {
            var pair = itemSelectorBuilders.get(i);
            if (priority > pair.getA())
                break;
            index++;
        }
        itemSelectorBuilders.add(index, new Pair<>(priority, selector));
    }

    public static void registerEntitySelectorBuilder(int priority, BiFunction<String, String, ASSelector<LivingEntity>> selector)
    {
        int index = 0;
        int size = entitySelectorBuilders.size();
        for (int i = 0; i < size; i++)
        {
            var pair = entitySelectorBuilders.get(i);
            if (priority > pair.getA())
                break;
            index++;
        }
        entitySelectorBuilders.add(index, new Pair<>(priority, selector));
    }

    public static void registerEntitySetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<LivingEntity>, ASSetter<LivingEntity>> builder)
    {
        int index = 0;
        int size = entitySetterBuilders.size();
        for (int i = 0; i < size; i++)
        {
            var pair = entitySetterBuilders.get(i);
            if (priority > pair.getA())
                break;
            index++;
        }
        entitySetterBuilders.add(index, new Pair<>(priority, builder));
    }

    public static void registerItemSetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<ItemStack>, ASSetter<ItemStack>> builder)
    {
        int index = 0;
        int size = itemSetterBuilders.size();
        for (int i = 0; i < size; i++)
        {
            var pair = itemSetterBuilders.get(i);
            if (priority > pair.getA())
                break;
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
