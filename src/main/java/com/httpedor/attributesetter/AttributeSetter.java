package com.httpedor.attributesetter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.attributesetter.compat.TrinketsCompat;
import com.httpedor.attributesetter.selectors.ASSelector;
import com.httpedor.attributesetter.selectors.CompositeASSelector;
import com.httpedor.attributesetter.selectors.entity.IdEntitySelector;
import com.httpedor.attributesetter.selectors.item.IdItemSelector;
import com.httpedor.attributesetter.setters.entity.EntityAttributeModifierSetter;
import com.httpedor.attributesetter.setters.entity.EntityAttributeSetter;
import com.httpedor.attributesetter.setters.item.ItemAttributeBaseSetter;
import com.httpedor.attributesetter.setters.item.ItemAttributeConversionSetter;
import com.httpedor.attributesetter.setters.item.ItemAttributeDependencySetter;
import com.httpedor.attributesetter.setters.item.ItemAttributeModifierSetter;
import com.httpedor.attributesetter.setters.item.ItemAttributeSetter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.ModifyItemAttributeModifiersCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.util.tuples.Pair;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AttributeSetter implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Identifier PACKET_ID = Identifier.of("attributesetter", "sync");

    public static Map<String, JsonObject> itemEntries = new HashMap<>();
    public static Map<String, JsonObject> entityEntries = new HashMap<>();

    private MinecraftServer server;

    @SuppressWarnings("unchecked")
    public static void handleItemJson(String defaultNamespace, JsonObject obj)
    {
        for (var entry : obj.entrySet())
        {
            List<Pair<JsonObject, String>> entries = new ArrayList<>();
            int index = 0;
            for (var modElement : entry.getValue().getAsJsonArray())
            {
                var modObj = modElement.getAsJsonObject();
                if (FabricLoader.getInstance().isModLoaded("trinkets") && TrinketsCompat.shouldCurioHandle(entry.getKey(), modObj))
                {
                    index++;
                    continue;
                }
                entries.add(new Pair<>(modObj, defaultNamespace + ":" + entry.getKey() + "/" + index));
                index++;
            }
            AttributeSetterAPI.registerItemEntry(entry.getKey(), entries.toArray(new Pair[0]), defaultNamespace);
        }
    }

    @SuppressWarnings("unchecked")
    public static void handleEntityJson(String defaultNamespace, JsonObject obj)
    {
        for (var entry : obj.entrySet())
        {
            List<Pair<JsonObject, String>> entries = new ArrayList<>();
            int index = 0;
            for (var modElement : entry.getValue().getAsJsonArray())
            {
                var modObj = modElement.getAsJsonObject();
                entries.add(new Pair<>(modObj, defaultNamespace + ":" + entry.getKey() + "/" + index));
                index++;
            }
            AttributeSetterAPI.registerEntityEntry(entry.getKey(), entries.toArray(new Pair[0]), defaultNamespace);
        }
    }

    private static IdItemSelector extractIdItemSelector(ASSelector<ItemStack> selector)
    {
        if (selector instanceof IdItemSelector idSelector)
            return idSelector;
        if (selector instanceof CompositeASSelector<ItemStack> composite)
        {
            for (var child : composite.selectors)
            {
                var found = extractIdItemSelector(child);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private static EquipmentSlot parseItemSlot(JsonElement slotElement, String id, ASSelector<ItemStack> selector)
    {
        if (slotElement == null)
        {
            var idSelector = extractIdItemSelector(selector);
            if (idSelector != null)
            {
                var itemEntry = Registries.ITEM.get(idSelector.id);
                if (itemEntry instanceof ArmorItem ai)
                    return ai.getSlotType();
            }
            return EquipmentSlot.MAINHAND;
        }

        try {
            return EquipmentSlot.valueOf(slotElement.getAsString().toUpperCase());
        } catch (IllegalArgumentException e)
        {
            LOGGER.error("Invalid slot: {} in entry {}", slotElement.getAsString(), id);
            return null;
        }
    }

    private static void setupSelectors()
    {
        AttributeSetterAPI.registerEntitySelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
            String namespace = fileName;
            if (str.contains(":"))
            {
                var parts = str.split(":", 2);
                namespace = parts[0];
                str = parts[1];
            }
            return new IdEntitySelector(new Identifier(namespace, str));
        });
        AttributeSetterAPI.registerItemSelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
            String namespace = fileName;
            if (str.contains(":"))
            {
                var parts = str.split(":", 2);
                namespace = parts[0];
                str = parts[1];
            }
            return new IdItemSelector(new Identifier(namespace, str));
        });

        AttributeSetterAPI.registerEntitySelectorBuilder(Integer.MAX_VALUE - 1, (str, fileName) -> {
            if (str.startsWith("!"))
            {
                var actualStr = str.substring(1).trim();
                var subSelector = AttributeSetterAPI.parseEntitySelector(actualStr, fileName);
                if (subSelector != null)
                {
                    subSelector.inverted = true;
                    return subSelector;
                }
            }
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(Integer.MAX_VALUE - 1, (str, fileName) -> {
            if (str.startsWith("!"))
            {
                var actualStr = str.substring(1).trim();
                var subSelector = AttributeSetterAPI.parseItemSelector(actualStr, fileName);
                if (subSelector != null)
                {
                    subSelector.inverted = true;
                    return subSelector;
                }
            }
            return null;
        });

        AttributeSetterAPI.registerEntitySelectorBuilder(Integer.MAX_VALUE, (str, fileName) -> {
            CompositeASSelector.Mode mode;
            String delimiter;
            if (str.contains("||"))
            {
                mode = CompositeASSelector.Mode.OR;
                delimiter = "\\|\\|";
            }
            else if (str.contains("&&"))
            {
                mode = CompositeASSelector.Mode.AND;
                delimiter = "&&";
            }
            else
            {
                return null;
            }

            var parts = str.split(delimiter);
            var selectors = new ArrayList<ASSelector<LivingEntity>>();
            for (var part : parts)
            {
                var sel = AttributeSetterAPI.parseEntitySelector(part.trim(), fileName);
                if (sel != null)
                    selectors.add(sel);
            }
            if (!selectors.isEmpty())
                return new CompositeASSelector<>(selectors, mode);
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(Integer.MAX_VALUE, (str, fileName) -> {
            CompositeASSelector.Mode mode;
            String delimiter;
            if (str.contains("||"))
            {
                mode = CompositeASSelector.Mode.OR;
                delimiter = "\\|\\|";
            }
            else if (str.contains("&&"))
            {
                mode = CompositeASSelector.Mode.AND;
                delimiter = "&&";
            }
            else
            {
                return null;
            }

            var parts = str.split(Pattern.quote(delimiter));
            var selectors = new ArrayList<ASSelector<ItemStack>>();
            for (var part : parts)
            {
                var sel = AttributeSetterAPI.parseItemSelector(part.trim(), fileName);
                if (sel != null)
                    selectors.add(sel);
            }
            if (!selectors.isEmpty())
                return new CompositeASSelector<>(selectors, mode);
            return null;
        });

        AttributeSetterAPI.registerEntitySelectorBuilder(50, (str, fileName) -> {
            if (str.startsWith("#"))
                return new com.httpedor.attributesetter.selectors.entity.TagEntitySelector(new Identifier(str.substring(1)));
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(50, (str, fileName) -> {
            if (str.startsWith("#"))
                return new com.httpedor.attributesetter.selectors.item.TagItemSelector(new Identifier(str.substring(1)));
            return null;
        });

        AttributeSetterAPI.registerEntitySelectorBuilder(100, (str, fileName) -> {
            var nbtStartIndex = str.indexOf('{');
            var nbtEndIndex = str.lastIndexOf('}');
            if (nbtStartIndex != -1 && nbtEndIndex != -1 && nbtEndIndex > nbtStartIndex)
            {
                String beforePart = str.substring(0, nbtStartIndex).trim();
                String nbtPart = str.substring(nbtStartIndex, nbtEndIndex + 1);
                ASSelector<LivingEntity> beforeSelector = beforePart.isEmpty() ? null : AttributeSetterAPI.parseEntitySelector(beforePart, fileName);
                if (beforeSelector == null && !beforePart.isEmpty())
                    return null;
                try
                {
                    if (beforeSelector != null)
                        return new CompositeASSelector<>(new ASSelector[] { beforeSelector, new com.httpedor.attributesetter.selectors.entity.NbtEntitySelector(nbtPart) }, CompositeASSelector.Mode.AND);
                    return new com.httpedor.attributesetter.selectors.entity.NbtEntitySelector(nbtPart);
                }
                catch (Exception ex)
                {
                    LOGGER.error("Failed to parse NBT selector part '{}'", nbtPart, ex);
                    return null;
                }
            }
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(100, (str, fileName) -> {
            var nbtStartIndex = str.indexOf('{');
            var nbtEndIndex = str.lastIndexOf('}');
            if (nbtStartIndex != -1 && nbtEndIndex != -1 && nbtEndIndex > nbtStartIndex)
            {
                String beforePart = str.substring(0, nbtStartIndex).trim();
                String nbtPart = str.substring(nbtStartIndex, nbtEndIndex + 1);
                ASSelector<ItemStack> beforeSelector = beforePart.isEmpty() ? null : AttributeSetterAPI.parseItemSelector(beforePart, fileName);
                if (beforeSelector == null && !beforePart.isEmpty())
                    return null;
                try
                {
                    if (beforeSelector != null)
                        return new CompositeASSelector<>(new ASSelector[] { beforeSelector, new com.httpedor.attributesetter.selectors.item.NbtItemSelector(nbtPart) }, CompositeASSelector.Mode.AND);
                    return new com.httpedor.attributesetter.selectors.item.NbtItemSelector(nbtPart);
                }
                catch (Exception ex)
                {
                    LOGGER.error("Failed to parse NBT selector part '{}'", nbtPart, ex);
                    return null;
                }
            }
            return null;
        });

        AttributeSetterAPI.registerEntitySelectorBuilder(99, (str, fileName) -> {
            var prefix = "regex:";
            if (str.contains(prefix))
            {
                var regex = str.substring(str.indexOf(prefix) + prefix.length()).trim();
                return new com.httpedor.attributesetter.selectors.entity.RegexEntitySelector(regex);
            }
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(99, (str, fileName) -> {
            var prefix = "regex:";
            if (str.contains(prefix))
            {
                var regex = str.substring(str.indexOf(prefix) + prefix.length()).trim();
                return new com.httpedor.attributesetter.selectors.item.RegexItemSelector(regex);
            }
            return null;
        });
    }

    private static void setupSetters()
    {
        AttributeSetterAPI.registerEntitySetterBuilder(0, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            var opElement = obj.get("operation");
            if (attrElement == null || valueElement == null)
                return null;

            var attr = Registries.ATTRIBUTE.get(new Identifier(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null)
            {
                LOGGER.error("Failed to find attribute {}", attrElement.getAsString());
                return null;
            }
            if (opElement == null || opElement.getAsString().equalsIgnoreCase("base"))
                return new EntityAttributeSetter(attr, value);

            try
            {
                var op = EntityAttributeModifier.Operation.valueOf(opElement.getAsString().toUpperCase());
                return new EntityAttributeModifierSetter(attr, op, value, id);
            }
            catch (Exception ex)
            {
                LOGGER.error("Failed to parse operation {}", opElement.getAsString());
                return null;
            }
        });

        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement != null && opElement.getAsString().equalsIgnoreCase("durability"))
                return null;
            return null;
        });

        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("base"))
                return null;
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            if (attrElement == null || valueElement == null)
                return null;
            var attr = Registries.ATTRIBUTE.get(new Identifier(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null)
            {
                LOGGER.error("Failed to find attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }
            var slot = parseItemSlot(obj.get("slot"), id, selector);
            if (slot == null)
                return null;
            return new ItemAttributeBaseSetter(attr, value, slot, id);
        });

        AttributeSetterAPI.registerItemSetterBuilder(0, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            var opElement = obj.get("operation");
            if (attrElement == null || valueElement == null)
                return null;
            if (opElement != null)
            {
                var opStr = opElement.getAsString();
                if (opStr.equalsIgnoreCase("base") || opStr.equalsIgnoreCase("durability") || opStr.equalsIgnoreCase("conversion") || opStr.equalsIgnoreCase("dependency"))
                    return null;
            }
            var attr = Registries.ATTRIBUTE.get(new Identifier(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null)
            {
                LOGGER.error("Failed to find attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }
            var slot = parseItemSlot(obj.get("slot"), id, selector);
            if (slot == null)
                return null;
            if (opElement == null)
                return new ItemAttributeModifierSetter(attr, EntityAttributeModifier.Operation.ADDITION, value, slot, id);
            try
            {
                var op = EntityAttributeModifier.Operation.valueOf(opElement.getAsString().toUpperCase());
                return new ItemAttributeModifierSetter(attr, op, value, slot, id);
            }
            catch (Exception ex)
            {
                LOGGER.error("Failed to parse operation {} in entry {}", opElement.getAsString(), id);
                return null;
            }
        });

        AttributeSetterAPI.registerItemSetterBuilder(0, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("conversion"))
                return null;
            var fromElement = obj.get("from");
            var attrElement = obj.get("attribute");
            if (fromElement == null || attrElement == null)
                return null;
            var fromAttr = Registries.ATTRIBUTE.get(new Identifier(fromElement.getAsString()));
            var toAttr = Registries.ATTRIBUTE.get(new Identifier(attrElement.getAsString()));
            if (fromAttr == null || toAttr == null)
                return null;
            var amountConverted = obj.has("amount") ? obj.get("amount").getAsFloat() : 1.0f;
            var conversionRate = obj.has("rate") ? obj.get("rate").getAsFloat() : 1.0f;
            return new ItemAttributeConversionSetter(fromAttr, toAttr, amountConverted, conversionRate, id);
        });

        AttributeSetterAPI.registerItemSetterBuilder(0, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("dependency"))
                return null;
            var attrElement = obj.get("attribute");
            var dependencyElement = obj.get("dependency");
            if (attrElement == null || dependencyElement == null)
                return null;
            var attr = Registries.ATTRIBUTE.get(new Identifier(attrElement.getAsString()));
            var dependency = Registries.ATTRIBUTE.get(new Identifier(dependencyElement.getAsString()));
            if (attr == null || dependency == null)
                return null;
            var multiplier = obj.has("multiplier") ? obj.get("multiplier").getAsFloat() : 1.0f;
            return new ItemAttributeDependencySetter(attr, dependency, multiplier, id);
        });
    }

    public static void onEntitySpawn(Entity entity, World world)
    {
        if (world.isClient)
            return;
        if (!(entity instanceof LivingEntity le))
            return;
        if (((ASLivingEntity) le).as$isLoaded())
            return;

        ((ASLivingEntity) le).as$setLoaded();
        for (var entry : AttributeSetterAPI.getEntriesFor(le))
            entry.apply(le);

        le.setHealth(le.getMaxHealth());
    }

    @Override
    public void onInitialize() {
        setupSelectors();
        setupSetters();

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> onEntitySpawn(entity, world));

        ModifyItemAttributeModifiersCallback.EVENT.register((stack, slot, modsMap) -> {
            for (var entry : AttributeSetterAPI.getEntriesFor(stack))
            {
                if (entry instanceof ItemAttributeSetter itemAttributeSetter)
                    itemAttributeSetter.apply(stack, slot, modsMap);
                else if (entry.shouldApply(stack))
                    entry.apply(stack);
            }
        });

        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(PACKET_ID, createPacketBuf()));

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier("attributesetter", "read_attributes");
            }

            @Override
            public void reload(ResourceManager manager) {
                entityEntries.clear();
                itemEntries.clear();
                AttributeSetterAPI.clearAll();

                for (Map.Entry<Identifier, Resource> resEntry : manager.findResources("attributesetter/entity", path -> true).entrySet())
                {
                    String fPath = resEntry.getKey().getPath();
                    String fName = fPath.substring(fPath.lastIndexOf('/') + 1, fPath.lastIndexOf('.'));
                    try (InputStream stream = manager.getResource(resEntry.getKey()).get().getInputStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                        entityEntries.put(fName, obj);
                        handleEntityJson(fName, obj);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read {}", resEntry.getKey(), e);
                    }
                }

                for (Map.Entry<Identifier, Resource> resEntry : manager.findResources("attributesetter/item", path -> true).entrySet())
                {
                    String fPath = resEntry.getKey().getPath();
                    String fName = fPath.substring(fPath.lastIndexOf('/') + 1, fPath.lastIndexOf('.'));
                    try (InputStream stream = manager.getResource(resEntry.getKey()).get().getInputStream()) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                        itemEntries.put(fName, obj);
                        handleItemJson(fName, obj);
                    } catch (Exception e) {
                        LOGGER.error("Failed to read {}", resEntry.getKey(), e);
                    }
                }

                if (server == null)
                    return;

                PacketByteBuf buf = createPacketBuf();
                for (var player : PlayerLookup.all(server))
                    ServerPlayNetworking.send(player, PACKET_ID, buf);
            }
        });
    }

    private static PacketByteBuf createPacketBuf() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(entityEntries.size());
        for (var entry : entityEntries.entrySet())
        {
            buf.writeString(entry.getKey());
            buf.writeString(entry.getValue().toString());
        }
        buf.writeInt(itemEntries.size());
        for (var entry : itemEntries.entrySet())
        {
            buf.writeString(entry.getKey());
            buf.writeString(entry.getValue().toString());
        }
        return buf;
    }
}