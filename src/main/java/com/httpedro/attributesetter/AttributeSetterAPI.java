package com.httpedro.attributesetter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.lang3.function.TriFunction;

import com.google.gson.JsonObject;
import com.httpedro.attributesetter.api.AttributeInjector;
import com.httpedro.attributesetter.api.RemovalRegistry;
import com.httpedro.attributesetter.api.TrueDefaults;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.entity.EntityTypeResolver;
import com.httpedro.attributesetter.selectors.entity.IdEntitySelector;
import com.httpedro.attributesetter.selectors.item.IdItemSelector;
import com.httpedro.attributesetter.setters.ASSetter;
import com.httpedro.attributesetter.setters.entity.EntityRemoveSetter;
import com.httpedro.attributesetter.setters.item.ItemSetter;

public class AttributeSetterAPI {
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<ItemStack>>>> itemSelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<LivingEntity>>>> entitySelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<Attribute>>>> attributeSelectorBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<LivingEntity>, ASSetter<LivingEntity>>>> entitySetterBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<ItemStack>, ASSetter<ItemStack>>>> itemSetterBuilders = new LinkedList<>();
    private static final LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<Attribute>, ASSetter<Attribute>>>> attributeSetterBuilders = new LinkedList<>();

    // ArrayLists so registration/iteration is O(1) per index and the caches below can address entries by position.
    static final List<Pair<ASSelector<LivingEntity>, ASSetter<LivingEntity>[]>> entitySetters = new ArrayList<>();
    // We cache ID selectors separately for performance, because they are exact matches and it'd be useless to iterate over all other selectors,
    // this is kind of a hacky way to implement it but it saves so much performance it's worth it
    static final Map<ResourceLocation, List<ASSetter<LivingEntity>>> entityIdCache = new HashMap<>();
    static final List<Pair<ASSelector<ItemStack>, ASSetter<ItemStack>[]>> itemSetters = new ArrayList<>();
    // Same as above
    static final Map<ResourceLocation, List<ASSetter<ItemStack>>> itemIdCache = new HashMap<>();

    // Per-type memoization of cacheable selectors. bit i is set when itemSetters/entitySetters entry i is a
    // cacheable selector that matches this Item/EntityType. Non-cacheable selectors are always evaluated live.
    // Concurrent maps: getEntriesFor is called from both the client render thread and the server thread.
    private static final Map<Item, BitSet> itemCacheBits = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<EntityType<?>, BitSet> entityCacheBits = new java.util.concurrent.ConcurrentHashMap<>();

    public static ASSelector<ItemStack> parseItemSelector(String selectorString, String fileName)
    {
        for (var builder : itemSelectorBuilders)
        {
            try {
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                    return selector;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing item selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }
    public static ASSelector<LivingEntity> parseEntitySelector(String selectorString, String fileName)
    {
        for (var builder : entitySelectorBuilders)
        {
            try {
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                    return selector;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing entity selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }
    public static ASSelector<Attribute> parseAttributeSelector(String selectorString, String fileName)
    {
        for (var builder : attributeSelectorBuilders)
        {
            try {
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                    return selector;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing attribute selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }

    public static ASSetter<ItemStack> parseItemSetter(JsonObject obj, String id, ASSelector<ItemStack> selector)
    {
        for (var builder : itemSetterBuilders)
        {
            try {
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                    return setter;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing item setter with id '{}':", id, e);
            }
        }
        return null;
    }
    public static ASSetter<LivingEntity> parseEntitySetter(JsonObject obj, String id, ASSelector<LivingEntity> selector)
    {
        for (var builder : entitySetterBuilders)
        {
            try {
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                    return setter;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing entity setter with id '{}':", id, e);
            }
        }
        return null;
    }
    public static ASSetter<Attribute> parseAttributeSetter(JsonObject obj, String id, ASSelector<Attribute> selector)
    {
        for (var builder : attributeSetterBuilders)
        {
            try {
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                    return setter;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing attribute setter with id '{}':", id, e);
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

        // Removals have to be known before anything spawns (spawn eggs, spawn placement checks), so resolve the
        // selector into entity types right away instead of waiting for a matching entity.
        for (var setter : setters)
        {
            if (!(setter instanceof EntityRemoveSetter))
                continue;
            var types = EntityTypeResolver.resolve(asSelector);
            if (types.isEmpty())
                Attributesetter.LOGGER.debug("Entity removal selector '{}' could not be resolved to entity types; its spawn eggs will be left alone", selector);
            for (var type : types)
                RemovalRegistry.removeEntityType(type);
            break;
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

    /**
     * Attribute entries are applied immediately (they register global attribute injections rather than being
     * consulted per entity/stack), so this resolves every matching attribute and runs the setters right away.
     */
    public static void registerAttributeEntry(String selectorString, Pair<JsonObject, String>[] entries, String fileName)
    {
        var selector = parseAttributeSelector(selectorString, fileName);
        if (selector == null)
        {
            Attributesetter.LOGGER.warn("Could not find a valid attribute selector for selector string '{}'", selectorString);
            return;
        }

        List<ASSetter<Attribute>> setters = new ArrayList<>();
        for (var entry : entries)
        {
            var setter = parseAttributeSetter(entry.getA(), entry.getB(), selector);
            if (setter == null)
            {
                Attributesetter.LOGGER.warn("Could not find a valid attribute entry builder for entry '{}'", entry.getB().toString());
                continue;
            }
            setters.add(setter);
        }

        for (var attribute : ForgeRegistries.ATTRIBUTES.getValues())
        {
            if (!selector.test(attribute))
                continue;
            for (var setter : setters)
                setter.apply(attribute);
        }
    }

    private static BitSet computeItemBits(Item item)
    {
        BitSet bits = new BitSet(itemSetters.size());
        ItemStack probe = new ItemStack(item);
        for (int i = 0; i < itemSetters.size(); i++)
        {
            var selector = itemSetters.get(i).getA();
            if (selector.canCache() && selector.test(probe))
                bits.set(i);
        }
        return bits;
    }

    public static List<ASSetter<ItemStack>> getEntriesFor(ItemStack stack)
    {
        var item = stack.getItem();
        List<ASSetter<ItemStack>> result = new ArrayList<>();
        var id = ForgeRegistries.ITEMS.getKey(item);
        var idc = itemIdCache.get(id);
        if (idc != null)
            result.addAll(idc);

        BitSet bits = itemCacheBits.computeIfAbsent(item, AttributeSetterAPI::computeItemBits);
        int itemSettersSize = itemSetters.size();
        for (int i = 0; i < itemSettersSize; i++)
        {
            var pair = itemSetters.get(i);
            boolean match = pair.getA().canCache() ? bits.get(i) : pair.getA().test(stack);
            if (match)
            {
                for (var setter : pair.getB())
                    result.add(setter);
            }
        }
        return result;
    }

    public static List<ASSetter<LivingEntity>> getEntriesFor(LivingEntity entity)
    {
        var type = entity.getType();
        List<ASSetter<LivingEntity>> result = new ArrayList<>();
        var id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        var idc = entityIdCache.get(id);
        if (idc != null)
            result.addAll(idc);

        // Cacheable selectors depend only on the entity type, so any instance of that type is a valid sample.
        BitSet bits = entityCacheBits.computeIfAbsent(type, t -> {
            BitSet b = new BitSet(entitySetters.size());
            for (int i = 0; i < entitySetters.size(); i++)
            {
                var selector = entitySetters.get(i).getA();
                if (selector.canCache() && selector.test(entity))
                    b.set(i);
            }
            return b;
        });
        int entitySettersSize = entitySetters.size();
        for (int i = 0; i < entitySettersSize; i++)
        {
            var pair = entitySetters.get(i);
            boolean match = pair.getA().canCache() ? bits.get(i) : pair.getA().test(entity);
            if (match)
            {
                for (var setter : pair.getB())
                    result.add(setter);
            }
        }
        return result;
    }

    /**
     * Applies item-global setters (max stack, durability, food) to every matching item. Called at the end of a
     * reload after {@link TrueDefaults#restoreAll()} so items whose entries were removed revert to vanilla.
     */
    public static void applyGlobalItemSetters()
    {
        applyGlobalItemSetters(true);
    }

    /**
     * @param serverSide false when this runs on a client receiving the datapack sync: the removals that only make
     *                   sense on the server (recipe stripping) were already done by the server before it sent them.
     */
    public static void applyGlobalItemSetters(boolean serverSide)
    {
        TrueDefaults.restoreAll();
        for (var item : ForgeRegistries.ITEMS.getValues())
        {
            ItemStack stack = new ItemStack(item);
            for (var setter : getEntriesFor(stack))
            {
                if (setter instanceof ItemSetter is && is.isGlobal())
                    is.apply(stack);
            }
        }
        RemovalRegistry.finishReload(serverSide);
    }

    /**
     * Registers a new item selector parser
     * @param priority The priority of the parser, higher priority parsers are checked first
     * @param selector The selector parser function. The first parameter is the selector string, the second is the file name
     */
    public static void registerItemSelectorBuilder(int priority, BiFunction<String, String, ASSelector<ItemStack>> selector)
    {
        insertByPriority(itemSelectorBuilders, priority, selector);
    }
    /**
     * Registers a new entity selector parser
     * @param priority The priority of the parser, higher priority parsers are checked first
     * @param selector The selector parser function. The first parameter is the selector string, the second is the file name
     */
    public static void registerEntitySelectorBuilder(int priority, BiFunction<String, String, ASSelector<LivingEntity>> selector)
    {
        insertByPriority(entitySelectorBuilders, priority, selector);
    }
    /**
     * Registers a new attribute selector parser
     * @param priority The priority of the parser, higher priority parsers are checked first
     * @param selector The selector parser function. The first parameter is the selector string, the second is the file name
     */
    public static void registerAttributeSelectorBuilder(int priority, BiFunction<String, String, ASSelector<Attribute>> selector)
    {
        insertByPriority(attributeSelectorBuilders, priority, selector);
    }

    /**
     * Registers a new entity setter builder
     * @param priority The priority of the builder, higher priority builders are checked first
     * @param builder The setter builder function. The first parameter is the JSON object, the second is the entry ID, the third is the selector. You can assume the ID is unique. The selector may be null.
     */
    public static void registerEntitySetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<LivingEntity>, ASSetter<LivingEntity>> builder)
    {
        insertByPriority(entitySetterBuilders, priority, builder);
    }

    /**
     * Registers a new item setter builder
     * @param priority The priority of the builder, higher priority builders are checked first
     * @param builder The setter builder function. The first parameter is the JSON object, the second is the entry ID, the third is the selector. You can assume the ID is unique. The selector may be null.
     */
    public static void registerItemSetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<ItemStack>, ASSetter<ItemStack>> builder)
    {
        insertByPriority(itemSetterBuilders, priority, builder);
    }

    /**
     * Registers a new attribute setter builder
     * @param priority The priority of the builder, higher priority builders are checked first
     * @param builder The setter builder function. The first parameter is the JSON object, the second is the entry ID, the third is the selector. You can assume the ID is unique. The selector may be null.
     */
    public static void registerAttributeSetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<Attribute>, ASSetter<Attribute>> builder)
    {
        insertByPriority(attributeSetterBuilders, priority, builder);
    }

    private static <B> void insertByPriority(LinkedList<Pair<Integer, B>> list, int priority, B value)
    {
        int index = 0;
        for (var pair : list)
        {
            if (priority > pair.getA())
                break;
            index++;
        }
        list.add(index, new Pair<>(priority, value));
    }

    public static void clearAll()
    {
        entitySetters.clear();
        entityIdCache.clear();
        itemSetters.clear();
        itemIdCache.clear();
        itemCacheBits.clear();
        entityCacheBits.clear();
        RemovalRegistry.clear();
        AttributeInjector.clearAll();
    }
}
