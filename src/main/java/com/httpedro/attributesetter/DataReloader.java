package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedro.attributesetter.api.AttributeSetterAPI;

import com.httpedro.attributesetter.api.BlockDefaults;
import com.httpedro.attributesetter.api.RemovalRegistry;
import com.httpedro.attributesetter.api.TrueDefaults;
import com.httpedro.attributesetter.api.UniqueRegistry;
import com.httpedro.attributesetter.selectors.entity.EntityTypeResolver;
import com.httpedro.attributesetter.setters.entity.EntityRemoveSetter;
import com.httpedro.attributesetter.setters.entity.EntityUniqueSetter;
import com.httpedro.attributesetter.setters.itemstack.ItemStackRemoveSetter;
import com.httpedro.attributesetter.setters.itemstack.ItemStackUniqueSetter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import oshi.util.tuples.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DataReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
    public Map<ResourceLocation, JsonElement> entries = new HashMap<>();

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
        var mode = splitted[0];
        var fName = splitted[splitted.length - 1];
        var obj = jsonElement.getAsJsonObject();
        int entryNum = 0;
        for (var entry : obj.entrySet())
        {
            int i = 0;
            var mods = entry.getValue().getAsJsonArray();
			Pair<JsonObject, String>[] modsWithIds = new Pair[mods.size()];
            for (var modElement : mods)
            {
                var selector = entry.getKey();
                if (!selector.matches("[a-z0-9/._-]"))
                {
                    selector = selector.replaceAll("[^a-z0-9/._-]", entryNum + "");
                }
                var entryPath = res.getNamespace() + "/" + fName + "/" + selector + "/" + i;
                var modJson = modElement.getAsJsonObject();
                modsWithIds[i] = new Pair<>(modJson, entryPath);
                i++;
            }
            AttributeSetterAPI.getTargetType(mode).registerEntry(entry.getKey(), modsWithIds, fName);
            entryNum++;
        }

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

        for (var item : BuiltInRegistries.ITEM)
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
        }
        // Block field setters (hardness, blast resistance). Remove/replace register into
        // BlockReplacementRegistry, and mining speed rides the BreakSpeed event, so both are naturally
        // skipped here - getGenericEntriesFor never returns the event setters.
        for (var block : BuiltInRegistries.BLOCK)
        {
            for (var entry : TargetTypes.BLOCK.getGenericEntriesFor(block))
            {
                entry.apply(block);
            }
        }
        for (var attribute : BuiltInRegistries.ATTRIBUTE)
        {
            for (var entry : TargetTypes.ATTRIBUTE.getGenericEntriesFor(attribute))
            {
                entry.apply(attribute);
            }
        }

        RemovalRegistry.finishReload(serverSide);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Attributesetter.LOGGER.info("Reloading attributesetter, found {} files", resourceLocationJsonElementMap.size());
        load(resourceLocationJsonElementMap, true);
    }
}
