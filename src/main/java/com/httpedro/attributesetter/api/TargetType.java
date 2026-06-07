package com.httpedro.attributesetter.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.commons.lang3.function.TriFunction;

import com.google.gson.JsonObject;
import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.setters.ASEventSetter;
import com.httpedro.attributesetter.setters.ASSetter;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import oshi.util.tuples.Pair;

//FIXME: Right now, my caching method doesn't support two different event targets in the same event class.
// This is because, to avoid iterating through all the entries for each event, It just assumes the first target it finds in the cacheable entries for
// that event is the same target for all other entries.
public abstract class TargetType<T, TCache>
{
    public static class Entry<T>
    {
        public ASSelector<T> selector;
        public ASSetter<T>[] setters;
        public Map<Class<? extends ASSetter<T>>, List<ASSetter<T>>> settersByClass = new HashMap<>();

        @SuppressWarnings("unchecked")
		public Entry(ASSelector<T> selector, ASSetter<T>[] setters) {
            this.selector = selector;
            this.setters = setters;

            for (var setter : setters)
            {
                Class<? extends ASSetter<T>> currentClass = (Class<? extends ASSetter<T>>)setter.getClass();
                while (ASSetter.class.isAssignableFrom(currentClass))
                {
                    var list = settersByClass.get(currentClass);
                    if (list == null)
                    {
                        list = new LinkedList<>();
                        settersByClass.put((Class<? extends ASSetter<T>>) currentClass, list);
                    }
                    list.add(setter);
                    currentClass = (Class<? extends ASSetter<T>>)currentClass.getSuperclass();
                }
            }
        }
    }
    // Event class -> List of entries that have a setter for that event.
    private Set<Class<?>> registeredEvents = new HashSet<>();
    public final Class<T> typeClass;

    LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<T>>>> selectorBuilders = new LinkedList<>();
    LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<T>, ASSetter<T>>>> setterBuilders = new LinkedList<>();

    Map<Class<? extends Event>, List<Entry<T>>> entriesByEvents = new HashMap<>();
    List<Entry<T>> entries = new LinkedList<>();
    Map<Class<? extends Event>, List<Entry<T>>> cacheableEntriesByEvents = new HashMap<>();
    List<Entry<T>> cacheableEntries = new LinkedList<>();

    Map<Class<? extends Event>, Map<TCache, List<ASEventSetter<T, ? extends Event>>>> cacheByEvent = new HashMap<>();
    Map<TCache, List<ASSetter<T>>> genericCache = new HashMap<>();

    protected TargetType(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    public ASSelector<T> parseSelector(String selectorString, String fileName)
    {
        int selectorBuildersSize = selectorBuilders.size();
        for (int i = 0; i < selectorBuildersSize; i++)
        {
            try {
                var builder = selectorBuilders.get(i);
                var selector = builder.getB().apply(selectorString, fileName);
                if (selector != null)
                {
                    return selector;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing selector string '{}' in file '{}':", selectorString, fileName, e);
            }
        }
        return null;
    }
    public ASSetter<T> parseSetter(JsonObject obj, String id, ASSelector<T> selector)
    {
        int setterBuildersSize = setterBuilders.size();
        for (int i = 0; i < setterBuildersSize; i++)
        {
            try {
                var builder = setterBuilders.get(i);
                var setter = builder.getB().apply(obj, id, selector);
                if (setter != null)
                {
                    return setter;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing setter with id '{}' in file '{}':", id, obj.toString(), e);
            }
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void registerEntry(String selectorString, Pair<JsonObject, String>[] newEntries, String fileName)
    {
        var selector = parseSelector(selectorString, fileName);
        if (selector == null)
        {
            Attributesetter.LOGGER.warn("Could not find a valid selector for selector string '{}'", selectorString);
            return;
        }

        Map<Class<? extends Event>, List<ASSetter<T>>> eventSetters = new HashMap<>();
        List<ASSetter<T>> genericSetters = new ArrayList<>();
        for (var entry : newEntries)
        {
            var id = entry.getB();
            var setter = parseSetter(entry.getA(), id, selector);
            if (setter == null)
            {
                Attributesetter.LOGGER.warn("Could not find a valid entry builder for entry '{}'", entry.getB().toString());
                continue;
            }

            if (setter instanceof ASEventSetter asEventSetter)
                eventSetters.computeIfAbsent(asEventSetter.getEventClass(), k -> new ArrayList<>()).add(setter);
            else
                genericSetters.add(setter);
        }

        List<Entry<T>> targetList;
        if (selector.canCache())
            targetList = cacheableEntries;
        else
            targetList = this.entries;

        // Insert based on selector specificity(lower specificity at the start, higher at the end, so more specific selectors are checked last)
        // This way, more specific selectors can override less specific ones
        int index = 0;
        int entriesSize = targetList.size();
        for (int i = 0; i < entriesSize; i++)
        {
            var entry = targetList.get(i);
            if (entry.selector.getSpecificity() > selector.getSpecificity())
            {
                break;
            }
            index++;
        }
        targetList.add(index, new Entry<>(selector, genericSetters.toArray(new ASSetter[0])));
        for (var eventMapEntry : eventSetters.entrySet())
        {
            var eventClass = eventMapEntry.getKey();
            var settersForEvent = eventMapEntry.getValue();
            var relevantMap = selector.canCache() ? cacheableEntriesByEvents : entriesByEvents;
            var list = relevantMap.get(eventClass);
            if (list == null)
            {
                list = new LinkedList<>();
                relevantMap.put(eventClass, list);
            }
            var eventEntry = new Entry<>(selector, settersForEvent.toArray(new ASSetter[0]));
            list.add(eventEntry);
            if (!registeredEvents.contains(eventClass))
            {
                NeoForge.EVENT_BUS.addListener(eventClass, (e) -> {
                    var castEvent = eventClass.cast(e);
                    var setters = getEntriesForEvent(eventClass, castEvent);
                    for (var setter : setters)
                    {
                        try {
                            setter.apply(castEvent);
                        } catch (Exception ex) {
                            Attributesetter.LOGGER.error("Error while applying setter '{}' for event '{}':", setter.getClass().getName(), eventClass.getName(), ex);
                        }
                    }
                });
                registeredEvents.add(eventClass);
            }
        }
    }

    public <TEvent extends Event> List<ASEventSetter<T, TEvent>> getEntriesForEvent(Class<? extends TEvent> eventClass, TEvent event)
    {
        List<ASEventSetter<T, TEvent>> result = new ArrayList<>();
        var entriesForEvent = entriesByEvents.get(eventClass);
        if (entriesForEvent != null)
        {
            for (var entry : entriesForEvent)
            {
                // We can be sure that all setters in this entry are ASEventSetters for this event,
                // since registerEntry only adds to entriesByEvents after checking that the setters are ASEventSetters for this event.
                // We can also be sure that there's at least one setter in this entry, since registerEntry doesn't add empty entries to entriesByEvents.
                // We cannot however, be sure that all setters have the same target, since for example:
                // - setter "crafting_multiplier" applies itself to the craft event, on the result
                // - setter "crafting_cost_multiplier" applies itself to the craft event, on the cost
                // Both will have the same event class, but different targets.
                T target = ((ASEventSetter<T, TEvent>)entry.setters[0]).getTarget(event);
                if (entry.selector.test(target))
                {
                    for (var setter : entry.setters)
                        result.add((ASEventSetter<T, TEvent>)setter);
                }
            }
        }
        // We can skip checking the cache if there are no cacheable entries for this event.
        if (cacheableEntriesByEvents.containsKey(eventClass) && cacheableEntriesByEvents.get(eventClass).size() > 0)
        {
            // Same reasoning as above. If there's a cacheable entry for this event, we can be sure that there's at least one setter
            // and all setters in cacheableEntriesByEvents are ASEventSetters
            T target = ((ASEventSetter<T, TEvent>)cacheableEntriesByEvents.get(eventClass).getFirst().setters[0]).getTarget(event);
            var cacheForEvent = cacheByEvent.computeIfAbsent(eventClass, k -> new HashMap<>());
            var cacheKey = getCacheKey(target);
            var cacheResult = cacheForEvent.get(cacheKey);
            if (cacheResult == null)
            {
                cacheResult = new LinkedList<>();
                var cacheableEntriesForEvent = cacheableEntriesByEvents.get(eventClass);
                if (cacheableEntriesForEvent != null)
                {
                    for (var entry : cacheableEntriesForEvent)
                    {
                        // Same reasoning as above.
                        if (entry.selector.test(target))
                        {
                            for (var setter : entry.setters)
                            {
                                cacheResult.add((ASEventSetter<T, ?>)setter);
                                result.add((ASEventSetter<T, TEvent>)setter);
                            }
                        }
                    }
                }
                cacheForEvent.put(cacheKey, cacheResult);
            }
            else
            {
                for (var setter : cacheResult)
                {
                    // No need to test the selector again, since this cache is made specifically for this event and target, so all selectors should have already been tested when the cache was built
                    result.add((ASEventSetter<T, TEvent>)setter);
                }
            }
        }
        return result;
    }

    public List<ASSetter<T>> getGenericEntriesFor(T object)
    {
        List<ASSetter<T>> result = new ArrayList<>();
        var cacheKey = getCacheKey(object);
        var cacheResult = genericCache.get(cacheKey);
        if (cacheResult == null)
        {
            cacheResult = new LinkedList<>();
            for (var entry : cacheableEntries)
            {
                if (entry.selector.test(object))
                {
                    for (var setter : entry.setters)
                    {
                        cacheResult.add(setter);
                        result.add(setter);
                    }
                }
            }
            genericCache.put(cacheKey, cacheResult);
        }
        else
            result.addAll(cacheResult);
        int entriesSize = entries.size();
        for (int i = 0; i < entriesSize; i++)
        {
            var entry = entries.get(i);
            if (entry.selector.test(object))
            {
                for (var setter : entry.setters)
                {
                    result.add(setter);
                }
            }
        }
        return result;
    }

    /**
     * Registers a new selector parser
     * @param priority The priority of the parser, higher priority parsers are checked first
     * @param selector The selector parser function. The first parameter is the selector string, the second is the file name
     */
    public void registerSelectorBuilder(int priority, BiFunction<String, String, ASSelector<T>> selector)
    {
        int index = 0;
        for (int i = 0; i < selectorBuilders.size(); i++)
        {
            var pair = selectorBuilders.get(i);
            if (priority > pair.getA())
            {
                break;
            }
            index++;
        }
        selectorBuilders.add(index, new Pair<>(priority, selector));
    }

    /**
     * Registers a new setter builder
     * @param priority The priority of the builder, higher priority builders are checked first
     * @param builder The setter builder function. The first parameter is the JSON object, the second is the entry ID, the third is the selector. You can assume the ID is unique. The selector may be null.
     */
    public void registerSetterBuilder(int priority, TriFunction<JsonObject, String, ASSelector<T>, ASSetter<T>> builder)
    {
        int index = 0;
        for (int i = 0; i < setterBuilders.size(); i++)
        {
            var pair = setterBuilders.get(i);
            if (priority > pair.getA())
            {
                break;
            }
            index++;
        }
        setterBuilders.add(index, new Pair<>(priority, builder));
    }

    public void clearEntries()
    {
        entries.clear();
        cacheableEntries.clear();
        genericCache.clear();
    }

    public void clearBuilders()
    {
        selectorBuilders.clear();
        setterBuilders.clear();
    }

    public abstract TCache getCacheKey(T object);
    public abstract String getFolderName();
}
