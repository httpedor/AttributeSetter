package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
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
        for (var entry : obj.entrySet())
        {
            int i = 0;
            var mods = entry.getValue().getAsJsonArray();
            Pair<JsonObject, String>[] modsWithIds = new Pair[mods.size()];
            for (var modElement : mods)
            {
                var entryPath = res.getNamespace() + ":" + fName + "/" + entry.getKey() + "/" + i;
                var modJson = modElement.getAsJsonObject();
                modsWithIds[i] = new Pair<>(modJson, entryPath);
                i++;
            }
            switch (mode) {
                case "entity":
                    AttributeSetterAPI.registerEntityEntry(entry.getKey(), modsWithIds, fName);
                    break;
                case "item":
                    AttributeSetterAPI.registerItemEntry(entry.getKey(), modsWithIds, fName);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        entries.clear();
        AttributeSetterAPI.clearAll();

        Attributesetter.LOGGER.info("Reloading attributesetter, found {} files", resourceLocationJsonElementMap.size());
        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : resourceLocationJsonElementMap.entrySet()) {
            addEntry(fileEntry.getKey(), fileEntry.getValue());
        }
    }
}

