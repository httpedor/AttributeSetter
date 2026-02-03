package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.httpedro.attributesetter.compat.CuriosCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
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
        try {
            switch (mode)
            {
                case "entity":
                {
                    int i = 0;
                    for (var entry : obj.entrySet())
                    {
                        var mods = entry.getValue().getAsJsonArray();
                        var selector = ASSelector.parse(entry.getKey(), fName.replace(".json", ""));
                        var entryPath = fName + "/" + entry.getKey() + "/" + i;
                        i++;
                        for (var modElement : mods)
                        {
                            var modObj = modElement.getAsJsonObject();
                            String opStr;
                            if (!modObj.has("operation"))
                                opStr = "BASE";
                            else
                                opStr = modObj.get("operation").getAsString();
                            var isBase = opStr.equalsIgnoreCase("base");
                            var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(modObj.get("attribute").getAsString()));
                            var value = modObj.get("value").getAsDouble();
                            if (attr == null)
                            {
                                Attributesetter.LOGGER.error("Failed to find attribute {}", modObj.get("attribute").getAsString());
                                continue;
                            }
                            ASEntry asentry;
                            if (isBase)
                                asentry = ASEntry.forEntity(res, EntryType.BASE, entryPath, attr, null, value);
                            else
                            {
                                var op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                asentry = ASEntry.forEntity(res, EntryType.MODIFIER, entryPath, attr, op, value);
                            }
                            AttributeSetterAPI.registerEntityEntry(selector, asentry);
                        }
                    }

                    break;
                }
                case "item":
                {
                    for (var entry : obj.entrySet())
                    {
                        var mods = entry.getValue().getAsJsonArray();
                        int i = 0;
                        for (var modElement : mods)
                        {
                            var entryPath = fName + "/" + entry.getKey() + "/" + i;
                            var modObj = modElement.getAsJsonObject();
                            if (ModList.get().isLoaded("curios") && CuriosCompat.shouldCurioHandle(entry.getKey(), modObj))
                            {
                                continue;
                            }
                            String opStr;
                            if (modObj.has("operation"))
                                opStr = modObj.get("operation").getAsString();
                            else
                                opStr = "ADDITION";
                            var isDur = opStr.equalsIgnoreCase("durability");

                            String slotStr = null;
                            if (modObj.has("slot"))
                                slotStr = modObj.get("slot").getAsString();

                            var value = modObj.get("value").getAsDouble();

                            ASSelector selector = ASSelector.parse(entry.getKey(), fName.replace(".json", ""));

                            EquipmentSlot slot = EquipmentSlot.MAINHAND;
                            if (!isDur)
                            {
                                try {
                                    if (slotStr == null)
                                    {
                                        var itemEntry = ForgeRegistries.ITEMS.getValue(selector.id);
                                        if (itemEntry instanceof ArmorItem ai)
                                            slot = ai.getEquipmentSlot();
                                    }
                                    else
                                    {
                                        slot = EquipmentSlot.valueOf(slotStr.toUpperCase());
                                    }
                                } catch (IllegalArgumentException e)
                                {
                                    Attributesetter.LOGGER.error("Invalid slot: {}", slotStr);
                                    continue;
                                }
                            }

                            Attribute attr = null;
                            if (!isDur)
                            {
                                attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(modObj.get("attribute").getAsString()));
                                if (attr == null)
                                {
                                    Attributesetter.LOGGER.error("Failed to find attribute {}", modObj.get("attribute").getAsString());
                                    continue;
                                }
                            }
                            //TODO: Handle UUIDs again
                            ASEntry asentry;
                            if (opStr.equalsIgnoreCase("base"))
                            {
                                asentry = ASEntry.forItem(res.getNamespace(), EntryType.BASE, entryPath, attr, slot, null, value);
                            }
                            else if (opStr.equalsIgnoreCase("durability"))
                            {
                                asentry = ASEntry.forItem(res.getNamespace(), EntryType.DURABILITY, entryPath, attr, slot, null, value);
                            }
                            else
                            {
                                var op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                asentry = ASEntry.forItem(res.getNamespace(), EntryType.MODIFIER, entryPath, attr, slot, op, value);
                            }

                            AttributeSetterAPI.registerItemEntry(selector, asentry);
                        }
                    }

                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        entries.clear();
        AttributeSetterAPI.clearAll();
        
        if (ModList.get().isLoaded("curios")) {
            CuriosCompat.clearMaps();
        }

        Attributesetter.LOGGER.info("Reloading attributesetter, found {} files", resourceLocationJsonElementMap.size());
        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : resourceLocationJsonElementMap.entrySet()) {
            addEntry(fileEntry.getKey(), fileEntry.getValue());
        }
    }
}
