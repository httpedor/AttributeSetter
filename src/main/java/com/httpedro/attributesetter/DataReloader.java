package com.httpedro.attributesetter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.UUID;

import static com.httpedro.attributesetter.Attributesetter.DEFAULT_UUID;

public class DataReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();

    public DataReloader() {
        super(GSON, "attributesetter");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        AttributeSetterAPI.ENTITY_MODIFIERS.clear();
        AttributeSetterAPI.BASE_MODIFIERS.clear();
        AttributeSetterAPI.TAG_MODIFIERS.clear();
        AttributeSetterAPI.BASE_TAG_MODIFIERS.clear();
        AttributeSetterAPI.ITEM_MODIFIERS.clear();
        AttributeSetterAPI.TAG_ITEM_MODIFIERS.clear();
        AttributeSetterAPI.BASE_ITEM_MODIFIERS.clear();

        System.out.println("Reloading attributesetter, found: " + resourceLocationJsonElementMap.size());
        for (Map.Entry<ResourceLocation, JsonElement> fileEntry : resourceLocationJsonElementMap.entrySet()) {
            var path = fileEntry.getKey().getPath();
            if (!path.contains("/"))
                continue;
            var mode = path.split("/")[0];
            var obj = fileEntry.getValue().getAsJsonObject();
            try {
                switch (mode)
                {
                    case "entity":
                    {
                        for (var entry : obj.entrySet())
                        {
                            var mods = entry.getValue().getAsJsonArray();
                            boolean isTag = entry.getKey().startsWith("#");
                            for (var modElement : mods)
                            {
                                var modObj = modElement.getAsJsonObject();
                                var opStr = modObj.get("operation").getAsString();
                                var isBase = opStr.toLowerCase().equals("base");
                                var id = isTag ? new ResourceLocation(entry.getKey().substring(1)) : new ResourceLocation(entry.getKey());
                                var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(modObj.get("attribute").getAsString()));
                                var value = modObj.get("value").getAsDouble();
                                if (attr == null)
                                {
                                    System.out.println("Failed to find attribute " + modObj.get("attribute").getAsString());
                                    continue;
                                }
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
                                    if (modObj.has("uuid"))
                                        mod = new AttributeModifier(UUID.fromString(modObj.get("uuid").getAsString()), "ASMod", value, op);
                                    else
                                        mod = new AttributeModifier(DEFAULT_UUID, "ASMod", value, op);

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
                            for (var modElement : mods)
                            {
                                var modObj = modElement.getAsJsonObject();
                                String opStr;
                                String slotStr;
                                if (modObj.has("operation"))
                                    opStr = modObj.get("operation").getAsString();
                                else
                                    opStr = "ADDITION";

                                if (modObj.has("slot"))
                                    slotStr = modObj.get("slot").getAsString();
                                else
                                    slotStr = "MAINHAND";

                                var id = isTag ? new ResourceLocation(entry.getKey().substring(1)) : new ResourceLocation(entry.getKey());
                                var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(modObj.get("attribute").getAsString()));
                                var value = modObj.get("value").getAsDouble();
                                EquipmentSlot slot;
                                try {
                                    slot = EquipmentSlot.valueOf(slotStr.toUpperCase());
                                } catch (IllegalArgumentException e)
                                {
                                    System.out.println("Invalid slot: " + slotStr);
                                    continue;
                                }
                                if (attr == null)
                                {
                                    System.out.println("Failed to find attribute " + modObj.get("attribute").getAsString());
                                    continue;
                                }
                                if (opStr.equalsIgnoreCase("base"))
                                {
                                    if (isTag)
                                        AttributeSetterAPI.registerTagBaseAttribute(id, attr, value);
                                    else
                                        AttributeSetterAPI.registerItemBaseAttribute(id, attr, value, slot);
                                }
                                else
                                {
                                    var op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                    AttributeModifier mod;
                                    if (modObj.has("uuid"))
                                        mod = new AttributeModifier(UUID.fromString(modObj.get("uuid").getAsString()), "ASMod", value, op);
                                    else
                                        mod = new AttributeModifier("ASMod", value, op);

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
    }
}
