package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.httpedro.attributesetter.compat.CuriosCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.httpedro.attributesetter.Attributesetter.DEFAULT_UUID;

public class DataReloader extends SimpleJsonResourceReloadListener {
    static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();
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
        var fName = splitted[1];
        var obj = jsonElement.getAsJsonObject();
        try {
            switch (mode)
            {
                case "entity":
                {
                    for (var entry : obj.entrySet())
                    {
                        var mods = entry.getValue().getAsJsonArray();
                        boolean isTag = entry.getKey().startsWith("#");
                        int i = -1;
                        for (var modElement : mods)
                        {
                            i++;
                            var modObj = modElement.getAsJsonObject();
                            String opStr;
                            if (!modObj.has("operation"))
                                opStr = "BASE";
                            else
                                opStr = modObj.get("operation").getAsString();
                            var isBase = opStr.equalsIgnoreCase("base");
                            var idStr = entry.getKey();
                            if (isTag)
                                idStr = idStr.substring(1);
                            var id = idStr.contains(":") ? ResourceLocation.parse(idStr) : ResourceLocation.fromNamespaceAndPath(fName, idStr);
                            var attrHolder = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(modObj.get("attribute").getAsString()));
                            var value = modObj.get("value").getAsDouble();
                            if (attrHolder.isEmpty())
                            {
                                Attributesetter.LOGGER.error("Failed to find attribute {}", modObj.get("attribute").getAsString());
                                continue;
                            }
                            var attr = attrHolder.get();
                            if (isBase)
                            {
                                if (isTag)
                                    AttributeSetterAPI.registerTagBaseAttribute(id, attr, modObj.get("value").getAsDouble());
                                else
                                    AttributeSetterAPI.registerEntityBaseAttribute(id, attr, modObj.get("value").getAsDouble());
                            }
                            else
                            {
                                var op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                AttributeModifier mod;
                                if (modObj.has("id"))
                                    mod = new AttributeModifier(ResourceLocation.parse(modObj.get("id").getAsString()), value, op);
                                else
                                    mod = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("AttributeSetter", idStr + "_" + i), value, op);

                                if (isTag)
                                    AttributeSetterAPI.registerTagAttributeModifier(id, attr, mod);
                                else
                                    AttributeSetterAPI.registerEntityAttributeModifier(id, attr, mod);
                            }
                        }
                    }

                    break;
                }
                case "item":
                {
                    for (var entry : obj.entrySet())
                    {
                        var mods = entry.getValue().getAsJsonArray();
                        boolean isTag = entry.getKey().startsWith("#");
                        int i = -1;
                        for (var modElement : mods)
                        {
                            i++;
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

                            String slotStr;
                            if (modObj.has("slot"))
                                slotStr = modObj.get("slot").getAsString();
                            else {
                                slotStr = null;
                            }

                            var idStr = entry.getKey();
                            if (isTag)
                                idStr = idStr.substring(1);
                            var id = idStr.contains(":") ? ResourceLocation.parse(idStr) : ResourceLocation.fromNamespaceAndPath(fName, idStr);
                            var value = modObj.get("value").getAsDouble();
                            EquipmentSlotGroup slot = null;
                            if (slotStr == null)
                            {
                                var itemEntry = BuiltInRegistries.ITEM.get(id);
                                if (itemEntry instanceof ArmorItem ai)
                                    EquipmentSlotGroup.bySlot(ai.getEquipmentSlot());
                                else
                                    slot = EquipmentSlotGroup.MAINHAND;
                            }
                            else
                            {
                                slot = Arrays.stream(EquipmentSlotGroup.values()).filter(group -> group.getSerializedName().toLowerCase().equals(slotStr)).findFirst().get();
                            }

                            if (slot == null)
                            {
                                Attributesetter.LOGGER.error("Invalid slot: {}", slotStr);
                                continue;
                            }

                            var attrOpt = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(modObj.get("attribute").getAsString()));
                            if (attrOpt.isEmpty())
                            {
                                Attributesetter.LOGGER.error("Failed to find attribute {}", modObj.get("attribute").getAsString());
                                continue;
                            }
                            var attr = attrOpt.get();
                            if (opStr.equalsIgnoreCase("base"))
                            {
                                if (isTag)
                                    AttributeSetterAPI.registerTagItemBaseAttribute(id, attr, value, slot);
                                else
                                    AttributeSetterAPI.registerItemBaseAttribute(id, attr, value, slot);
                            }
                            else
                            {
                                var op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                AttributeModifier mod;
                                if (modObj.has("id"))
                                    mod = new AttributeModifier(ResourceLocation.parse(modObj.get("id").getAsString()), value, op);
                                else
                                    mod = new AttributeModifier(ResourceLocation.fromNamespaceAndPath("AttributeSetter", idStr + "_" + i), value, op);

                                if (isTag)
                                    AttributeSetterAPI.registerTagItemAttributeModifier(id, attr, mod, slot);
                                else
                                    AttributeSetterAPI.registerItemAttributeModifier(id, attr, mod, slot);
                            }
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
        AttributeSetterAPI.ENTITY_MODIFIERS.clear();
        AttributeSetterAPI.BASE_MODIFIERS.clear();
        AttributeSetterAPI.TAG_MODIFIERS.clear();
        AttributeSetterAPI.BASE_TAG_MODIFIERS.clear();
        AttributeSetterAPI.ITEM_MODIFIERS.clear();
        AttributeSetterAPI.TAG_ITEM_MODIFIERS.clear();
        AttributeSetterAPI.BASE_ITEM_MODIFIERS.clear();

        Attributesetter.LOGGER.info("Reloading attributesetter, found {} files", resourceLocationJsonElementMap.size());
        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : resourceLocationJsonElementMap.entrySet()) {
            addEntry(fileEntry.getKey(), fileEntry.getValue());
        }
    }
}
