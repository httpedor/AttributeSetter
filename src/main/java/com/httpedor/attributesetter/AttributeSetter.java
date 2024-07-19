package com.httpedor.attributesetter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
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
            if (((ICEALivingEntity)le).cea$isLoaded())
                return;

            ((ICEALivingEntity) le).cea$setLoaded();
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

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("customentityattributes", "read_attributes");
            }

            @Override
            public void reload(ResourceManager manager) {
                AttributeSetterAPI.ENTITY_MODIFIERS.clear();
                AttributeSetterAPI.BASE_MODIFIERS.clear();
                AttributeSetterAPI.TAG_MODIFIERS.clear();
                AttributeSetterAPI.BASE_TAG_MODIFIERS.clear();
                //TODO add support for items
                for (Map.Entry<Identifier, Resource> resEntry : manager.findResources("attributesetter", path -> true).entrySet())
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
                                        mod = new EntityAttributeModifier(UUID.fromString(modObj.get("uuid").getAsString()), "CEAMod", value, op);
                                    else
                                        mod = new EntityAttributeModifier("CEAMod", value, op);

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
            }
        });
    }
}