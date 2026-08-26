package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedro.attributesetter.api.AttributeSetterAPI;

import com.httpedro.attributesetter.api.BlockDefaults;
import com.httpedro.attributesetter.api.RecipeProcessor;
import com.httpedro.attributesetter.api.RemovalRegistry;
import com.httpedro.attributesetter.api.TargetType;
import com.httpedro.attributesetter.api.TrueDefaults;
import com.httpedro.attributesetter.api.UniqueRegistry;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.entity.EntityTypeResolver;
import com.httpedro.attributesetter.setters.ASSetter;
import com.httpedro.attributesetter.setters.entity.EntityRemoveSetter;
import com.httpedro.attributesetter.setters.entity.EntityUniqueSetter;
import com.httpedro.attributesetter.setters.itemstack.ItemStackRemoveSetter;
import com.httpedro.attributesetter.setters.itemstack.ItemStackUniqueSetter;
import com.httpedro.attributesetter.setters.itemstack.attribute.ItemAttributeBaker;
import com.httpedro.attributesetter.util.JsonHelper;
import com.httpedro.attributesetter.util.RegistryValues;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
    public Map<ResourceLocation, JsonElement> entries = new HashMap<>();

    /** One selector plus the setters written for it, before either side has been parsed. */
    private record RawEntry(JsonElement selector, List<JsonElement> setters, String label) {}

    public DataReloader() {
        super(GSON, "attributesetter");
    }

    @Override
    public String getName() {
        return "AttributeSeter";
    }

    public void addEntry(ResourceLocation res, JsonElement jsonElement)
    {
        entries.put(res, jsonElement);
        var path = res.getPath();
        if (!path.contains("/"))
            return;
        var splitted = path.split("/");
        var folder = splitted[0];
        var fName = splitted[splitted.length - 1];

        var chain = AttributeSetterAPI.getTargetChain(folder);
        if (chain.isEmpty())
        {
            Attributesetter.LOGGER.warn("Unknown target folder '{}' (file {}); nothing in it will be applied", folder, res);
            return;
        }

        List<RawEntry> rawEntries;
        try {
            rawEntries = readFile(jsonElement, res);
        } catch (Exception e) {
            Attributesetter.LOGGER.error("Could not read attributesetter file {}:", res, e);
            return;
        }

        for (var raw : rawEntries)
            registerEntry(chain, raw, res.getNamespace() + "/" + fName + "/" + raw.label(), fName);
    }

    /**
     * Reads a file into selector/setters pairs. Both layouts are accepted:
     * <ul>
     *   <li>an object, whose keys are shorthand selectors - the usual, most compact form,</li>
     *   <li>an array of {@code {"selector": ..., "setters": ...}} objects, which is the only way to write a
     *       selector that is itself an object rather than a string.</li>
     * </ul>
     * In the object form a value that carries its own {@code selector} field turns its key into a plain label,
     * so complex selectors can still be used without switching the whole file over to the array form.
     */
    private List<RawEntry> readFile(JsonElement fileElement, ResourceLocation res)
    {
        List<RawEntry> result = new ArrayList<>();
        if (fileElement.isJsonArray())
        {
            int index = 0;
            for (var item : fileElement.getAsJsonArray())
            {
                if (!item.isJsonObject())
                {
                    Attributesetter.LOGGER.warn("Entry #{} of {} is not an object; skipping", index, res);
                    index++;
                    continue;
                }
                var entry = readEntryObject(item.getAsJsonObject(), String.valueOf(index), res);
                if (entry != null)
                    result.add(entry);
                index++;
            }
            return result;
        }

        if (!fileElement.isJsonObject())
        {
            Attributesetter.LOGGER.warn("{} is neither an object nor an array; skipping", res);
            return result;
        }

        int entryNum = 0;
        for (var entry : fileElement.getAsJsonObject().entrySet())
        {
            // JSON has no comments, so a leading underscore is the usual stand-in for one. Skipping those keys
            // means a note at the top of a file is not read as a selector that matches nothing.
            if (entry.getKey().startsWith("_"))
                continue;
            var label = sanitizeLabel(entry.getKey(), entryNum);
            var value = entry.getValue();
            if (value.isJsonObject() && JsonHelper.hasAny(value.getAsJsonObject(), "selector", "select", "target"))
            {
                // The key is just a name here; the real selector is inside.
                var parsed = readEntryObject(value.getAsJsonObject(), label, res);
                if (parsed != null)
                    result.add(parsed);
            }
            else
                result.add(new RawEntry(new com.google.gson.JsonPrimitive(entry.getKey()), JsonHelper.list(value), label));
            entryNum++;
        }
        return result;
    }

    private RawEntry readEntryObject(JsonObject obj, String label, ResourceLocation res)
    {
        var selector = JsonHelper.first(obj, "selector", "select", "target");
        if (selector == null)
        {
            Attributesetter.LOGGER.warn("Entry '{}' of {} has no selector; skipping", label, res);
            return null;
        }
        var setters = JsonHelper.first(obj, "setters", "apply", "modifiers", "operations");
        if (setters == null)
        {
            Attributesetter.LOGGER.warn("Entry '{}' of {} has no setters; skipping", label, res);
            return null;
        }
        var name = JsonHelper.string(obj, null, "name", "id");
        return new RawEntry(selector, JsonHelper.list(setters), name == null ? label : sanitizeLabel(name, 0));
    }

    /**
     * Hands each setter to the first target type in the chain that recognises it. This is what makes the
     * {@code item} folder take both per-stack and per-item operations: a durability setter is turned down by
     * the itemstack target and picked up by the item one, with a selector parsed for whichever claimed it.
     */
    private void registerEntry(List<TargetType<?, ?>> chain, RawEntry raw, String idBase, String fileName)
    {
        Map<TargetType<?, ?>, ASSelector<?>> selectors = new LinkedHashMap<>();
        Map<TargetType<?, ?>, List<ASSetter<?>>> claimed = new LinkedHashMap<>();
        boolean anySelector = false;

        for (int i = 0; i < raw.setters().size(); i++)
        {
            var setterElement = raw.setters().get(i);
            var id = idBase + "/" + i;
            boolean handled = false;

            for (var target : chain)
            {
                ASSelector<?> selector;
                if (selectors.containsKey(target))
                    selector = selectors.get(target);
                else
                {
                    selector = target.parseSelector(raw.selector(), fileName);
                    selectors.put(target, selector);
                }
                if (selector == null)
                    continue;
                anySelector = true;

                var setter = parseSetter(target, setterElement, id, selector);
                if (setter != null)
                {
                    claimed.computeIfAbsent(target, k -> new ArrayList<>()).add(setter);
                    handled = true;
                    break;
                }
            }

            if (!handled)
            {
                if (!anySelector)
                    Attributesetter.LOGGER.warn("Could not parse selector '{}' in file '{}'", raw.selector(), fileName);
                else
                    Attributesetter.LOGGER.warn("No setter in '{}' understands '{}'", idBase, setterElement);
            }
        }

        for (var entry : claimed.entrySet())
            register(entry.getKey(), selectors.get(entry.getKey()), entry.getValue());
    }

    @SuppressWarnings("unchecked")
    private static <T, C> ASSetter<T> parseSetter(TargetType<T, C> target, JsonElement element, String id, ASSelector<?> selector)
    {
        return target.parseSetter(element, id, (ASSelector<T>) selector);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void register(TargetType target, ASSelector<?> selector, List<ASSetter<?>> setters)
    {
        target.registerEntry(selector, (List) setters);
    }

    /**
     * Entry ids end up in attribute modifier ids, so they have to stay within what a ResourceLocation path
     * allows - and stay unique, which is what the entry number on the end is for (two selectors can easily
     * clean down to the same text).
     */
    private static String sanitizeLabel(String selector, int entryNum)
    {
        var cleaned = selector.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "");
        return cleaned.isEmpty() ? String.valueOf(entryNum) : cleaned + "." + entryNum;
    }

    /**
     * Resets everything and registers a whole set of files. Used both by the datapack reload and by the client
     * receiving the sync payload, so the two end up in the same state.
     */
    public void load(Map<ResourceLocation, JsonElement> files, boolean serverSide)
    {
        if (!TrueDefaults.isPopulated())
            TrueDefaults.populate();
        if (!BlockDefaults.isPopulated())
            BlockDefaults.populate();

        entries.clear();
        AttributeSetterAPI.clearAll();

        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : files.entrySet()) {
            addEntry(fileEntry.getKey(), fileEntry.getValue());
        }

        // Entity removals have to be resolved into entity types before anything spawns (spawn eggs, spawn
        // placement checks), and the recipe pass below needs the removed spawn eggs to already be known.
        for (var entry : TargetTypes.ENTITY.getAllEntries())
        {
            if (!entry.settersByClass.containsKey(EntityRemoveSetter.class))
                continue;
            var types = EntityTypeResolver.resolve(entry.selector);
            if (types.isEmpty())
                Attributesetter.LOGGER.debug("An entity removal selector could not be resolved to entity types; its spawn eggs will be left alone");
            for (var type : types)
                RemovalRegistry.removeEntityType(type);
        }

        // Entity unique caps are registered per resolvable type, the same way removals are (NBT/isEnemy-only
        // selectors can't be resolved without a live entity, so those entries won't gate - see DOCS).
        for (var entry : TargetTypes.ENTITY.getAllEntries())
        {
            var uniqueSetters = entry.settersByClass.get(EntityUniqueSetter.class);
            if (uniqueSetters == null || uniqueSetters.isEmpty())
                continue;
            var rule = ((EntityUniqueSetter) uniqueSetters.get(0)).getRule();
            var types = EntityTypeResolver.resolve(entry.selector);
            if (types.isEmpty())
                Attributesetter.LOGGER.debug("An entity make_unique selector could not be resolved to entity types; it will not gate spawns");
            for (var type : types)
                UniqueRegistry.registerEntity(type, rule);
        }

        for (var item : RegistryValues.of(BuiltInRegistries.ITEM))
        {
            for (var entry : TargetTypes.ITEM.getGenericEntriesFor(item))
            {
                entry.apply(item);
            }
            // Item removal is also accepted in the `item` folder (itemstack). Those setters are generic (not event
            // setters), so they are never applied per stack - we collect them here by probing each item.
            var probe = new ItemStack(item);
            for (var entry : TargetTypes.ITEMSTACK.getGenericEntriesFor(probe))
            {
                if (entry instanceof ItemStackRemoveSetter removeSetter)
                    removeSetter.apply(probe);
                else if (entry instanceof ItemStackUniqueSetter uniqueSetter)
                    uniqueSetter.apply(probe);
            }
            // Item-wide attribute setters get folded into the item's default ATTRIBUTE_MODIFIERS component so
            // that mods reading the component directly (Better Combat's off-hand swap, for one) see the same
            // values the ItemAttributeModifierEvent produces.
            ItemAttributeBaker.bake(item);
        }
        // Block field setters (hardness, blast resistance). Remove/replace register into
        // BlockReplacementRegistry, and mining speed rides the BreakSpeed event, so both are naturally
        // skipped here - getGenericEntriesFor never returns the event setters.
        for (var block : RegistryValues.of(BuiltInRegistries.BLOCK))
        {
            for (var entry : TargetTypes.BLOCK.getGenericEntriesFor(block))
            {
                entry.apply(block);
            }
        }
        for (var attribute : RegistryValues.of(BuiltInRegistries.ATTRIBUTE))
        {
            for (var entry : TargetTypes.ATTRIBUTE.getGenericEntriesFor(attribute))
            {
                entry.apply(attribute);
            }
        }

        // Recipe rewriting runs before the removal pass so that, if a recipe is retargeted onto a removed item,
        // the removal stripping below still catches it. Both only touch the recipe manager server-side; clients
        // get the finished recipe list through vanilla's own recipe sync.
        if (serverSide)
            RecipeProcessor.process();

        RemovalRegistry.finishReload(serverSide);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Attributesetter.LOGGER.info("Reloading attributesetter, found {} files", resourceLocationJsonElementMap.size());
        load(resourceLocationJsonElementMap, true);
    }
}
