package com.httpedor.attributesetter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.item.v1.ModifyItemAttributeModifiersCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;

public class AttributeSetter implements ModInitializer {

    @Override
    public void onInitialize() {

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (world.isClient) return;
            if (!(entity instanceof LivingEntity))
                return;
            LivingEntity le = (LivingEntity) entity;
            if (((ASLivingEntity)le).as$isLoaded())
                return;

            ((ASLivingEntity) le).as$setLoaded();
            var entityType = Registries.ENTITY_TYPE.getId(entity.getType());
            var id = new Identifier(entityType.getNamespace(), entityType.getPath());
            for (var entry : AttributeSetterAPI.BASE_TAG_MODIFIERS.entrySet())
            {
                if (le.getType().isIn(TagKey.of(RegistryKeys.ENTITY_TYPE, entry.getKey())))
                {
                    for (var modEntry : entry.getValue().entrySet())
                    {
                        var attrInstance = le.getAttributeInstance(modEntry.getKey());
                        if (attrInstance != null)
                            attrInstance.setBaseValue(modEntry.getValue());
                    }
                }
            }

            var baseMods = AttributeSetterAPI.BASE_MODIFIERS.getOrDefault(id, null);
            if (baseMods != null)
            {
                for (var entry : baseMods.entrySet())
                {
                    var attrInstance = le.getAttributeInstance(entry.getKey());
                    if (attrInstance != null)
                        attrInstance.setBaseValue(entry.getValue());
                }
            }

            for (var entry : AttributeSetterAPI.TAG_MODIFIERS.entrySet())
            {
                if (le.getType().isIn(TagKey.of(RegistryKeys.ENTITY_TYPE, entry.getKey())))
                {
                    for (var modEntry : entry.getValue().entrySet())
                    {
                        var attrInstance = le.getAttributeInstance(modEntry.getKey());
                        if (attrInstance != null)
                            attrInstance.addPersistentModifier(modEntry.getValue());
                    }
                }
            }

            var modifiers = AttributeSetterAPI.ENTITY_MODIFIERS.getOrDefault(id, null);
            if (modifiers != null)
            {
                for (var entry : modifiers.entrySet())
                {
                    var attrInstance = le.getAttributeInstance(entry.getKey());
                    if (attrInstance != null)
                        attrInstance.addPersistentModifier(entry.getValue());
                }
            }

            le.setHealth(le.getMaxHealth());
        });

        ModifyItemAttributeModifiersCallback.EVENT.register((stack, slot, modsMap) -> {
            var item = stack.getItem();
            var id = Registries.ITEM.getId(item);
            for (var entry : AttributeSetterAPI.BASE_TAG_ITEM_MODIFIERS.entrySet())
            {
                if (stack.isIn(TagKey.of(RegistryKeys.ITEM, entry.getKey()))
                        && entry.getValue().containsKey(slot))
                {
                    for (var modEntry : entry.getValue().get(slot).entrySet())
                    {
                        modsMap.get(modEntry.getKey()).clear();
                        modsMap.put(modEntry.getKey(), new EntityAttributeModifier("ASMod", modEntry.getValue(), EntityAttributeModifier.Operation.ADDITION));
                    }
                }
            }
            for (var entry : AttributeSetterAPI.BASE_ITEM_MODIFIERS.entrySet())
            {
                if (entry.getKey().equals(id) && entry.getValue().containsKey(slot))
                {
                    for (var modEntry : entry.getValue().get(slot).entrySet())
                    {
                        modsMap.get(modEntry.getKey()).clear();
                        modsMap.put(modEntry.getKey(), new EntityAttributeModifier("ASMod", modEntry.getValue(), EntityAttributeModifier.Operation.ADDITION));
                    }
                }
            }
            for (var entry : AttributeSetterAPI.TAG_ITEM_MODIFIERS.entrySet())
            {
                if (stack.isIn(TagKey.of(RegistryKeys.ITEM, entry.getKey())) && entry.getValue().containsKey(slot))
                {
                    for (var modEntry : entry.getValue().get(slot).entrySet())
                    {
                        modsMap.put(modEntry.getKey(), modEntry.getValue());
                    }
                }
            }

            var modifiers = AttributeSetterAPI.ITEM_MODIFIERS.getOrDefault(id, null);
            if (modifiers != null)
            {
                var slotMods = modifiers.getOrDefault(slot, null);
                if (slotMods != null)
                {
                    for (var entry : slotMods.entrySet())
                    {
                        modsMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        });

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("attributesetter", "read_attributes");
            }

            @Override
            public void reload(ResourceManager manager) {
                AttributeSetterAPI.ENTITY_MODIFIERS.clear();
                AttributeSetterAPI.BASE_MODIFIERS.clear();
                AttributeSetterAPI.TAG_MODIFIERS.clear();
                AttributeSetterAPI.BASE_TAG_MODIFIERS.clear();
                AttributeSetterAPI.ITEM_MODIFIERS.clear();
                AttributeSetterAPI.TAG_ITEM_MODIFIERS.clear();
                for (Map.Entry<Identifier, Resource> resEntry : manager.findResources("attributesetter/entity", path -> true).entrySet())
                {
                    try (InputStream stream = manager.getResource(resEntry.getKey()).get().getInputStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
                        for (var entry : obj.entrySet())
                        {
                            var mods = entry.getValue().getAsJsonArray();
                            boolean isTag = entry.getKey().startsWith("#");
                            for (var modElement : mods)
                            {
                                var modObj = modElement.getAsJsonObject();
                                var opStr = modObj.get("operation").getAsString();
                                var isBase = opStr.toLowerCase().equals("base");
                                var id = isTag ? new Identifier(entry.getKey().substring(1)) : new Identifier(entry.getKey());
                                var attr = Registries.ATTRIBUTE.get(new Identifier(modObj.get("attribute").getAsString()));
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
                                    var op = EntityAttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                    EntityAttributeModifier mod;
                                    if (modObj.has("uuid"))
                                        mod = new EntityAttributeModifier(UUID.fromString(modObj.get("uuid").getAsString()), "ASMod", value, op);
                                    else
                                        mod = new EntityAttributeModifier("ASMod", value, op);

                                    if (isTag)
                                        AttributeSetterAPI.registerTagAttributeModifier(id, attr, mod);
                                    else
                                        AttributeSetterAPI.registerEntityAttributeModifier(id, attr, mod);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to read " + resEntry.getKey());
                        e.printStackTrace();
                    }
                }

                for (Map.Entry<Identifier, Resource> resEntry : manager.findResources("attributesetter/item", path -> true).entrySet())
                {
                    try (InputStream stream = manager.getResource(resEntry.getKey()).get().getInputStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        JsonObject obj = (JsonObject) JsonParser.parseReader(reader);
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

                                var id = isTag ? new Identifier(entry.getKey().substring(1)) : new Identifier(entry.getKey());
                                var attr = Registries.ATTRIBUTE.get(new Identifier(modObj.get("attribute").getAsString()));
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
                                    var op = EntityAttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                    EntityAttributeModifier mod;
                                    if (modObj.has("uuid"))
                                        mod = new EntityAttributeModifier(UUID.fromString(modObj.get("uuid").getAsString()), "ASMod", value, op);
                                    else
                                        mod = new EntityAttributeModifier("ASMod", value, op);

                                    if (isTag)
                                        AttributeSetterAPI.registerTagItemAttributeModifier(id, attr, mod, slot);
                                    else
                                        AttributeSetterAPI.registerItemAttributeModifier(id, attr, mod, slot);
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Failed to read " + resEntry.getKey());
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
