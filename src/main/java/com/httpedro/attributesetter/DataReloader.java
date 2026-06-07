package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.httpedro.attributesetter.api.AttributeSetterAPI;

import com.httpedro.attributesetter.api.TrueDefaults;
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

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        if (!TrueDefaults.isPopulated())
            TrueDefaults.populate();

        entries.clear();
        AttributeSetterAPI.clearAll();

        Attributesetter.LOGGER.info("Reloading attributesetter, found {} files", resourceLocationJsonElementMap.size());
        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : resourceLocationJsonElementMap.entrySet()) {
            addEntry(fileEntry.getKey(), fileEntry.getValue());
        }

        for (var item : BuiltInRegistries.ITEM)
        {
            for (var entry : TargetTypes.ITEM.getGenericEntriesFor(item))
            {
                entry.apply(item);
            }
        }
        for (var attribute : BuiltInRegistries.ATTRIBUTE)
        {
            for (var entry : TargetTypes.ATTRIBUTE.getGenericEntriesFor(attribute))
            {
                entry.apply(attribute);
            }
        }
    }
}
