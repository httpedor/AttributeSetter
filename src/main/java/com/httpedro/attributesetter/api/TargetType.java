package com.httpedro.attributesetter.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.function.TriFunction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.CompositeASSelector;
import com.httpedro.attributesetter.selectors.ProjectedASSelector;
import com.httpedro.attributesetter.setters.ASEventSetter;
import com.httpedro.attributesetter.setters.ASSetter;
import com.httpedro.attributesetter.util.JsonHelper;
import com.httpedro.attributesetter.util.Shorthand;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import oshi.util.tuples.Pair;

//FIXME: Right now, my caching method doesn't support two different event targets in the same event class.
// This is because, to avoid iterating through all the entries for each event, It just assumes the first target it finds in the cacheable entries for
// that event is the same target for all other entries.
public abstract class TargetType<T, TCache>
{
    /**
     * Fields that steer how a selector object is read rather than describing what it matches. They are skipped
     * when an object without an explicit {@code type} is read as an implicit AND of its fields.
     */
    private static final Set<String> SELECTOR_CONTROL_KEYS = Set.of(
            "type", "selector_type", "invert", "inverted", "negate", "specificity", "priority",
            "comment", "_comment", "description"
    );

    /**
     * A link from a target type to the one it derives from: everything the parent can parse - selectors here,
     * and setters through the folder dispatch in the data reloader - is usable from the derived type too, by
     * projecting the object onto the parent first (an ItemStack onto its Item).
     */
    public static final class Derivation<T, P> {
        public final TargetType<P, ?> parent;
        public final Function<T, P> projection;

        Derivation(TargetType<P, ?> parent, Function<T, P> projection) {
            this.parent = parent;
            this.projection = projection;
        }
    }

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

    private Derivation<T, ?> derivation;

    LinkedList<Pair<Integer, BiFunction<String, String, ASSelector<T>>>> selectorBuilders = new LinkedList<>();
    Map<String, BiFunction<JsonObject, String, ASSelector<T>>> jsonSelectorBuilders = new LinkedHashMap<>();
    LinkedList<Pair<Integer, TriFunction<JsonObject, String, ASSelector<T>, ASSetter<T>>>> setterBuilders = new LinkedList<>();
    LinkedList<Pair<Integer, BiFunction<Shorthand, String, JsonObject>>> setterShorthands = new LinkedList<>();

    Map<Class<? extends Event>, List<Entry<T>>> entriesByEvents = new HashMap<>();
    List<Entry<T>> entries = new LinkedList<>();
    Map<Class<? extends Event>, List<Entry<T>>> cacheableEntriesByEvents = new HashMap<>();
    List<Entry<T>> cacheableEntries = new LinkedList<>();

    Map<Class<? extends Event>, Map<TCache, List<ASEventSetter<T, ? extends Event>>>> cacheByEvent = new HashMap<>();
    Map<TCache, List<ASSetter<T>>> genericCache = new HashMap<>();

    protected TargetType(Class<T> typeClass) {
        this.typeClass = typeClass;
    }

    /**
     * Declares that this target type is a more specific view of the given one. Selectors registered on the
     * parent become usable here (projected through the given function), and a datapack folder pointed at this
     * type also accepts the parent's setters - which is what lets a single item folder handle both per-stack
     * and per-item operations.
     */
    public <P> void derivesFrom(TargetType<P, ?> parent, Function<T, P> projection)
    {
        this.derivation = new Derivation<>(parent, projection);
    }

    public Derivation<T, ?> getDerivation()
    {
        return derivation;
    }

    /** @return the target type this one derives from, or null. */
    public TargetType<?, ?> getParent()
    {
        return derivation == null ? null : derivation.parent;
    }

    // ---------------------------------------------------------------------------------------------------
    // Selector parsing
    // ---------------------------------------------------------------------------------------------------

    /** Parses a selector written in the shorthand string form. */
    public ASSelector<T> parseSelector(String selectorString, String fileName)
    {
        if (selectorString == null)
            return null;
        var str = selectorString.trim();
        int selectorBuildersSize = selectorBuilders.size();
        for (int i = 0; i < selectorBuildersSize; i++)
        {
            try {
                var builder = selectorBuilders.get(i);
                var selector = builder.getB().apply(str, fileName);
                if (selector != null)
                {
                    return selector;
                }
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while parsing selector string '{}' in file '{}':", str, fileName, e);
            }
        }
        return derivation == null ? null : parseFromParent(derivation, str, fileName);
    }

    /**
     * Parses a selector in any of its written forms:
     * <ul>
     *   <li>a string - the shorthand ({@code "minecraft:stone"}, {@code "#c:ores"}, {@code "!regex:.*_ore"}),</li>
     *   <li>an array - matches when any of its entries match,</li>
     *   <li>an object with a {@code type} - the full form of one selector,</li>
     *   <li>an object without a {@code type} - every recognised field has to match at once.</li>
     * </ul>
     */
    public ASSelector<T> parseSelector(JsonElement element, String fileName)
    {
        if (element == null || element.isJsonNull())
            return null;
        if (element.isJsonPrimitive())
            return parseSelector(element.getAsString(), fileName);
        if (element.isJsonArray())
        {
            List<ASSelector<T>> parts = new ArrayList<>();
            for (var item : element.getAsJsonArray())
            {
                var part = parseSelector(item, fileName);
                if (part == null)
                {
                    Attributesetter.LOGGER.warn("Could not parse selector '{}' in file '{}'", item, fileName);
                    continue;
                }
                parts.add(part);
            }
            if (parts.isEmpty())
                return null;
            if (parts.size() == 1)
                return parts.get(0);
            return new CompositeASSelector<>(parts, CompositeASSelector.Mode.OR);
        }
        if (!element.isJsonObject())
            return null;

        var obj = element.getAsJsonObject();
        ASSelector<T> result;
        var typeElement = JsonHelper.first(obj, "type", "selector_type");
        if (typeElement != null && typeElement.isJsonPrimitive())
            result = buildJsonSelector(typeElement.getAsString(), obj, fileName);
        else
            result = buildImplicitSelector(obj, fileName);

        if (result == null)
            return null;

        if (JsonHelper.bool(obj, false, "invert", "inverted", "negate"))
            result.inverted = !result.inverted;
        var specificity = JsonHelper.first(obj, "specificity", "priority");
        if (specificity != null && specificity.isJsonPrimitive())
            result.specificityOverride = specificity.getAsFloat();
        return result;
    }

    /** Builds one named selector, falling back to the target type this one derives from. */
    public ASSelector<T> buildJsonSelector(String type, JsonObject obj, String fileName)
    {
        var key = JsonHelper.key(type);
        var builder = jsonSelectorBuilders.get(key);
        if (builder != null)
        {
            try {
                return builder.apply(obj, fileName);
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while building '{}' selector in file '{}':", key, fileName, e);
                return null;
            }
        }
        if (derivation != null)
            return buildFromParent(derivation, key, obj, fileName);
        Attributesetter.LOGGER.warn("Unknown selector type '{}' for target '{}' in file '{}'", key, getFolderName(), fileName);
        return null;
    }

    /** @return whether this type (or one it derives from) knows a selector called that. */
    public boolean hasJsonSelector(String type)
    {
        var key = JsonHelper.key(type);
        if (jsonSelectorBuilders.containsKey(key))
            return true;
        return derivation != null && derivation.parent.hasJsonSelector(key);
    }

    /**
     * An object with no type field is read as "all of these at once", so a selector carrying both a tag and an
     * nbt field means what it looks like. Sibling fields stay visible to each builder, which is how
     * per-selector options (ignore_case, match) get passed along.
     */
    private ASSelector<T> buildImplicitSelector(JsonObject obj, String fileName)
    {
        List<ASSelector<T>> parts = new ArrayList<>();
        for (var field : obj.entrySet())
        {
            var key = JsonHelper.key(field.getKey());
            if (SELECTOR_CONTROL_KEYS.contains(key) || !hasJsonSelector(key))
                continue;
            var view = obj.deepCopy();
            view.add("value", field.getValue());
            var part = buildJsonSelector(key, view, fileName);
            if (part != null)
                parts.add(part);
        }
        if (parts.isEmpty())
        {
            Attributesetter.LOGGER.warn("Selector object {} in file '{}' has no field this target ('{}') understands",
                    obj, fileName, getFolderName());
            return null;
        }
        if (parts.size() == 1)
            return parts.get(0);
        return new CompositeASSelector<>(parts, CompositeASSelector.Mode.AND);
    }

    private <P> ASSelector<T> parseFromParent(Derivation<T, P> d, String str, String fileName)
    {
        return project(d, d.parent.parseSelector(str, fileName));
    }

    private <P> ASSelector<T> buildFromParent(Derivation<T, P> d, String type, JsonObject obj, String fileName)
    {
        if (!d.parent.hasJsonSelector(type))
        {
            Attributesetter.LOGGER.warn("Unknown selector type '{}' for target '{}' in file '{}'", type, getFolderName(), fileName);
            return null;
        }
        return project(d, d.parent.buildJsonSelector(type, obj, fileName));
    }

    private <P> ASSelector<T> project(Derivation<T, P> d, ASSelector<P> parentSelector)
    {
        return parentSelector == null ? null : new ProjectedASSelector<>(parentSelector, d.projection);
    }

    // ---------------------------------------------------------------------------------------------------
    // Setter parsing
    // ---------------------------------------------------------------------------------------------------

    public ASSetter<T> parseSetter(JsonObject obj, String id, ASSelector<T> selector)
    {
        // `op` is an accepted shorthand for `operation`. Normalising it here means every setter builder can go
        // on reading `operation` without each having to know about the alias.
        if (obj != null && obj.has("op") && !obj.has("operation"))
            obj.add("operation", obj.get("op"));

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
                Attributesetter.LOGGER.error("Error while parsing setter with id '{}' ({}):", id, obj, e);
            }
        }
        return null;
    }

    /** Parses a setter written either as an object or as a shorthand string. */
    public ASSetter<T> parseSetter(JsonElement element, String id, ASSelector<T> selector)
    {
        if (element == null || element.isJsonNull())
            return null;
        JsonObject obj;
        if (element.isJsonPrimitive())
        {
            obj = expandSetterShorthand(element.getAsString(), id);
            if (obj == null)
                return null;
        }
        else if (element.isJsonObject())
            obj = element.getAsJsonObject();
        else
            return null;
        return parseSetter(obj, id, selector);
    }

    /**
     * Turns a shorthand string into the object form a setter builder expects. Operations that need it register
     * their own expansion; everything else goes through the generic one, which already covers the common
     * "remove" / "durability 500" / "hardness x0.5" shapes.
     */
    public JsonObject expandSetterShorthand(String str, String id)
    {
        var shorthand = Shorthand.parse(str);
        if (shorthand == null)
            return null;
        int size = setterShorthands.size();
        for (int i = 0; i < size; i++)
        {
            try {
                var expanded = setterShorthands.get(i).getB().apply(shorthand, id);
                if (expanded != null)
                    return expanded;
            } catch (Exception e) {
                Attributesetter.LOGGER.error("Error while expanding setter shorthand '{}' in entry '{}':", str, id, e);
            }
        }
        return shorthand.toJson();
    }

    // ---------------------------------------------------------------------------------------------------
    // Entries
    // ---------------------------------------------------------------------------------------------------

    /** Registers an already-built selector together with the setters that go with it. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void registerEntry(ASSelector<T> selector, List<ASSetter<T>> setters)
    {
        if (selector == null || setters == null || setters.isEmpty())
            return;

        Map<Class<? extends Event>, List<ASSetter<T>>> eventSetters = new HashMap<>();
        List<ASSetter<T>> genericSetters = new ArrayList<>();
        for (var setter : setters)
        {
            if (setter instanceof ASEventSetter asEventSetter)
                eventSetters.computeIfAbsent(asEventSetter.getEventClass(), k -> new ArrayList<>()).add(setter);
            else
                genericSetters.add(setter);
        }

        if (!genericSetters.isEmpty())
        {
            List<Entry<T>> targetList = selector.canCache() ? cacheableEntries : this.entries;
            targetList.add(insertionIndex(targetList, selector), new Entry<>(selector, genericSetters.toArray(new ASSetter[0])));
        }

        for (var eventMapEntry : eventSetters.entrySet())
        {
            var eventClass = eventMapEntry.getKey();
            var settersForEvent = eventMapEntry.getValue();
            var relevantMap = selector.canCache() ? cacheableEntriesByEvents : entriesByEvents;
            var list = relevantMap.computeIfAbsent(eventClass, k -> new LinkedList<>());
            var eventEntry = new Entry<>(selector, settersForEvent.toArray(new ASSetter[0]));
            list.add(insertionIndex(list, selector), eventEntry);
            if (!registeredEvents.contains(eventClass))
            {
                NeoForge.EVENT_BUS.addListener(eventClass, (e) -> {
                    var castEvent = eventClass.cast(e);
                    var settersToApply = getEntriesForEvent(eventClass, castEvent);
                    for (var setter : settersToApply)
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

    /**
     * Entries are kept sorted by selector specificity (least specific first), so a broad rule is applied before -
     * and therefore overridden by - a targeted one.
     */
    private int insertionIndex(List<Entry<T>> list, ASSelector<T> selector)
    {
        int index = 0;
        int size = list.size();
        for (int i = 0; i < size; i++)
        {
            if (list.get(i).selector.specificity() > selector.specificity())
                break;
            index++;
        }
        return index;
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

    /**
     * Every cacheable (target-only) entry registered for the given event class. Reload-time passes use this to fold
     * event setters into the target's defaults ahead of time, which is the only way code that reads the raw data
     * components instead of firing the event can see them.
     */
    public List<Entry<T>> getCacheableEntriesForEvent(Class<? extends Event> eventClass)
    {
        var list = cacheableEntriesByEvents.get(eventClass);
        return list == null ? List.of() : list;
    }

    /**
     * Every non-event entry currently registered, cacheable or not. Meant for reload-time passes that need to look
     * at the entries themselves (and their selectors) rather than at what matches a given object.
     */
    public List<Entry<T>> getAllEntries()
    {
        List<Entry<T>> all = new ArrayList<>(cacheableEntries.size() + entries.size());
        all.addAll(cacheableEntries);
        all.addAll(entries);
        return all;
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

    // ---------------------------------------------------------------------------------------------------
    // Builder registration
    // ---------------------------------------------------------------------------------------------------

    /**
     * Registers a new shorthand-string selector parser
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
     * Registers a selector that can be written as an object, under one or more type names. The same names double
     * as field names in a type-less selector object, so a "regex" type and a "regex" field reach the same builder.
     * @param builder receives the whole selector object (so sibling options stay readable) and the file name
     * @param names the type name, plus any aliases
     */
    public void registerJsonSelector(BiFunction<JsonObject, String, ASSelector<T>> builder, String... names)
    {
        for (var name : names)
            jsonSelectorBuilders.put(JsonHelper.key(name), builder);
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

    /**
     * Registers an expansion from a shorthand setter string to the object form. Return null to let the next
     * expansion (and ultimately the generic one) have a go.
     * @param priority higher priority expansions are checked first
     */
    public void registerSetterShorthand(int priority, BiFunction<Shorthand, String, JsonObject> expansion)
    {
        int index = 0;
        for (int i = 0; i < setterShorthands.size(); i++)
        {
            if (priority > setterShorthands.get(i).getA())
                break;
            index++;
        }
        setterShorthands.add(index, new Pair<>(priority, expansion));
    }

    public void clearEntries()
    {
        entries.clear();
        cacheableEntries.clear();
        genericCache.clear();
        // The event maps and their cache are keyed off the entries too - leaving them behind would keep the
        // previous datapack's event setters live after a reload dropped them. registeredEvents stays: the bus
        // listeners are permanent and look the entries up on every event.
        entriesByEvents.clear();
        cacheableEntriesByEvents.clear();
        cacheByEvent.clear();
    }

    public void clearBuilders()
    {
        selectorBuilders.clear();
        jsonSelectorBuilders.clear();
        setterBuilders.clear();
        setterShorthands.clear();
    }

    public abstract TCache getCacheKey(T object);

    /** The datapack folder this target type is read from, and the name it is registered under. */
    public abstract String getFolderName();

    /** Extra folder names that mean the same target type. */
    public List<String> getFolderAliases()
    {
        return List.of();
    }

    @Override
    public String toString()
    {
        return "TargetType[" + getFolderName() + "]";
    }
}
