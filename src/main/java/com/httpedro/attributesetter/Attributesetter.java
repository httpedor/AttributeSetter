package com.httpedro.attributesetter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.httpedro.attributesetter.compat.CuriosCompat;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.CompositeASSelector;
import com.httpedro.attributesetter.selectors.entity.IdEntitySelector;
import com.httpedro.attributesetter.selectors.entity.NbtEntitySelector;
import com.httpedro.attributesetter.selectors.entity.RegexEntitySelector;
import com.httpedro.attributesetter.selectors.entity.TagEntitySelector;
import com.httpedro.attributesetter.selectors.item.IdItemSelector;
import com.httpedro.attributesetter.selectors.item.NbtItemSelector;
import com.httpedro.attributesetter.selectors.item.RegexItemSelector;
import com.httpedro.attributesetter.selectors.item.TagItemSelector;
import com.httpedro.attributesetter.setters.entity.EntityAttributeModifierSetter;
import com.httpedro.attributesetter.setters.entity.EntityAttributeSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeBaseSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeConversionSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeDependencySetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeModifierSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeSetter;
import com.httpedro.attributesetter.setters.item.ItemDurabilitySetter;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Attributesetter.MODID)
public class Attributesetter {
    public static final UUID DEFAULT_UUID = UUID.fromString("21ef99f1-c77a-42cf-ba8f-a59cf69ce7a6");
    public static final UUID BASE_UUID = UUID.fromString("b697bf19-6a3a-4baf-89ce-5d4a3422a3a4");
    public static boolean isApothic = false;

    // Define mod id in a common place for everything to reference
    private static final DataReloader dr = new DataReloader();
    public static final String MODID = "attributesetter";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1.0",
            s -> true,
            s -> true
    );
    public Attributesetter() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        if (ModList.get().isLoaded("curios"))
        {
            var compat = new CuriosCompat();
            compat.bootstrap();
            MinecraftForge.EVENT_BUS.register(compat);
        }
    }

    @SuppressWarnings("unchecked")
    public void commonSetup(FMLCommonSetupEvent e)
    {
        setupSelectors();
        setupSetters();
        isApothic = ModList.get().isLoaded("attributeslib");
        e.enqueueWork(() -> {
            CHANNEL.registerMessage(0, HashMap.class,
                    (map, buf) -> {
                        HashMap<ResourceLocation, JsonElement> entries = (HashMap<ResourceLocation, JsonElement>) map;
                        buf.writeMap(entries, FriendlyByteBuf::writeResourceLocation, (buf1, el) -> buf1.writeUtf(el.toString()));
                    },
                    (buf) -> {
                        HashMap<ResourceLocation, JsonElement> entries = new HashMap<>();
                        buf.readMap(i -> entries, FriendlyByteBuf::readResourceLocation, (res) -> JsonParser.parseString(res.readUtf()));
                        return entries;
                    },
                    (map, contextSupplier) -> {
                        AttributeSetterAPI.clearAll();
                        for (Object obj : map.entrySet())
                        {
                            var entry = (Map.Entry<ResourceLocation, JsonElement>) obj;
                            dr.addEntry(entry.getKey(), entry.getValue());
                        }
                    }
            );
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void datapackReload(AddReloadListenerEvent e)
    {
        e.addListener(dr);
    }

    @SubscribeEvent
    public void syncData(OnDatapackSyncEvent e)
    {
        for (var p : e.getPlayers())
        {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), dr.entries);
        }
    }

    @SubscribeEvent
    public void onItemAttribute(ItemAttributeModifierEvent e)
    {
        var stack = e.getItemStack();

        for (var entry : AttributeSetterAPI.getEntriesFor(stack))
        {
            if (entry instanceof ItemAttributeSetter ias)
            {
                ias.apply(e);
            }
            else if (entry.shouldApply(stack))
                entry.apply(stack);
        }

    }

    private void processEntity(LivingEntity le)
    {
        final var entries = AttributeSetterAPI.getEntriesFor(le);
        if (le.getType() == EntityType.PLAYER)
        {
            for (var entry : entries)
            {
                if (entry instanceof EntityAttributeSetter eas)
                {
                    eas.apply(le);
                }
            }
        }
        if (((ASLivingEntity)le).as$isLoaded())
            return;

        ((ASLivingEntity)le).as$setLoaded();

        for (var entry : entries)
        {
            entry.apply(le);
        }
        le.setHealth(le.getMaxHealth());
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent e)
    {
        var world = e.getLevel();
        var entity = e.getEntity();
        if (world.isClientSide)
            return;
        if (!(entity instanceof LivingEntity le))
            return;
        processEntity(le);
    }

    @SubscribeEvent
    public void onEntitySpawn(MobSpawnEvent.FinalizeSpawn e)
    {
        var world = e.getLevel();
        var entity = e.getEntity();
        if (world.isClientSide())
            return;

        processEntity(entity);
    }
    @SubscribeEvent
    public void onEntityBred(BabyEntitySpawnEvent e)
    {
        var world = e.getChild().level();
        if (world.isClientSide())
            return;
        processEntity(e.getChild());
    }

    //TODO: Selectors that get re-checkd on certain events (like NBT changes)
    private void setupSelectors()
    {
        // ID selectors
        AttributeSetterAPI.registerEntitySelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
            String namespace = fileName;
            if (str.contains(":"))
            {
                var parts = str.split(":");
                namespace = parts[0];
                str = parts[1];
            }
            var res = new ResourceLocation(namespace, str);
            return new IdEntitySelector(res);
        });
        AttributeSetterAPI.registerItemSelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
            String namespace = fileName;
            if (str.contains(":"))
            {
                var parts = str.split(":");
                namespace = parts[0];
                str = parts[1];
            }
            var res = new ResourceLocation(namespace, str);
            return new IdItemSelector(res);
        });

        // Composite selectors
        AttributeSetterAPI.registerItemSelectorBuilder(Integer.MAX_VALUE - 1, (String str, String fileName) -> {
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
        AttributeSetterAPI.registerEntitySelectorBuilder(Integer.MAX_VALUE - 1, (String str, String fileName) -> {
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
        AttributeSetterAPI.registerEntitySelectorBuilder(Integer.MAX_VALUE, (String str, String fileName) -> {
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
                return null;

            var parts = str.split(delimiter);
            var selectors = new ArrayList<ASSelector<LivingEntity>>();
            for (var part : parts)
            {
                var sel = AttributeSetterAPI.parseEntitySelector(part.trim(), fileName);
                if (sel != null)
                    selectors.add(sel);
            }
            if (selectors.size() > 0)
                return new CompositeASSelector<LivingEntity>(selectors, mode);

            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(Integer.MAX_VALUE, (String str, String fileName) -> {
            CompositeASSelector.Mode mode;
            String delimiter;
            if (str.contains("||"))
            {
                mode = CompositeASSelector.Mode.OR;
                delimiter = "||";
            }
            else if (str.contains("&&"))
            {
                mode = CompositeASSelector.Mode.AND;
                delimiter = "&&";
            }
            else
                return null;

            var parts = str.split(Pattern.quote(delimiter));
            var selectors = new ArrayList<ASSelector<ItemStack>>();
            for (var part : parts)
            {
                var sel = AttributeSetterAPI.parseItemSelector(part.trim(), fileName);
                if (sel != null)
                    selectors.add(sel);
            }
            if (selectors.size() > 0)
                return new CompositeASSelector<ItemStack>(selectors, mode);

            return null;
        });

        // Tag selectors
        AttributeSetterAPI.registerEntitySelectorBuilder(50, (str, fileName) -> {
            if (str.startsWith("#"))
            {
                var tag = str.substring(1);
                var tagRes = new ResourceLocation(tag);
                return new TagEntitySelector(tagRes);
            }
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(50, (str, fileName) -> {
            if (str.startsWith("#"))
            {
                var tag = str.substring(1);
                var tagRes = new ResourceLocation(tag);
                return new TagItemSelector(tagRes);
            }
            return null;
        });

        // Nbt selectors
        AttributeSetterAPI.registerEntitySelectorBuilder(100, (str, fileName) -> {
            var nbtStartIndex = str.indexOf('{');
            var nbtEndIndex = str.lastIndexOf('}');
            if (nbtStartIndex != -1 && nbtEndIndex != -1 && nbtEndIndex > nbtStartIndex)
            {
                String beforePart = str.substring(0, nbtStartIndex).trim();
                String nbtPart = str.substring(nbtStartIndex, nbtEndIndex + 1);
                ASSelector<LivingEntity> beforeSelector;
                if (beforePart.isEmpty())
                    beforeSelector = null;
                else
                    beforeSelector = AttributeSetterAPI.parseEntitySelector(beforePart, fileName);
                if (beforeSelector == null && !beforePart.isEmpty())
                    return null;
                try {
                    if (beforeSelector != null)
                    {
                        return new CompositeASSelector<>(new ASSelector[] {
                                beforeSelector,
                                new NbtEntitySelector(nbtPart)
                        }, CompositeASSelector.Mode.AND);
                    }
                    else
                        return new NbtEntitySelector(nbtPart);
                } catch (Exception ex)
                {
                    Attributesetter.LOGGER.error("Failed to parse NBT selector part '{}'", nbtPart, ex);
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
                ASSelector<ItemStack> beforeSelector;
                if (beforePart.isEmpty())
                    beforeSelector = null;
                else
                    beforeSelector = AttributeSetterAPI.parseItemSelector(beforePart, fileName);
                if (beforeSelector == null && !beforePart.isEmpty())
                    return null;
                try {
                    if (beforeSelector != null)
                    {
                        return new CompositeASSelector<>(new ASSelector[] {
                                beforeSelector,
                                new NbtItemSelector(nbtPart)
                        }, CompositeASSelector.Mode.AND);
                    }
                    else
                        return new NbtItemSelector(nbtPart);
                } catch (Exception ex)
                {
                    Attributesetter.LOGGER.error("Failed to parse NBT selector part '{}'", nbtPart, ex);
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
                return new RegexEntitySelector(regex);
            }
            return null;
        });
        AttributeSetterAPI.registerItemSelectorBuilder(99, (str, fileName) -> {
            var prefix = "regex:";
            if (str.contains(prefix))
            {
                var regex = str.substring(str.indexOf(prefix) + prefix.length()).trim();
                return new RegexItemSelector(regex);
            }
            return null;
        });
    }
    private void setupSetters()
    {
        // Simple attribute setters
        AttributeSetterAPI.registerEntitySetterBuilder(0, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            var opElement = obj.get("operation");
            if (attrElement != null && valueElement != null)
            {
                var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attrElement.getAsString()));
                var value = valueElement.getAsDouble();
                if (attr == null)
                {
                    Attributesetter.LOGGER.error("Failed to find attribute {}", attrElement.getAsString());
                    return null;
                }
                if (opElement == null || opElement.getAsString().equalsIgnoreCase("base"))
                    return new EntityAttributeSetter(attr, value);
                else
                {
                    try
                    {
                        var op = AttributeModifier.Operation.valueOf(opElement.getAsString().toUpperCase());
                        return new EntityAttributeModifierSetter(attr, op, value, id);
                    } catch (Exception ex)
                    {
                        Attributesetter.LOGGER.error("Failed to parse operation {}", opElement.getAsString());
                        return null;
                    }
                }
            }
            return null;
        });

        // Item setters: split by operation
        // - priority 1: durability/base (checked first)
        // - priority 0: modifier (default)

        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("durability"))
                return null;
            var valueElement = obj.get("value");
            if (valueElement == null)
                return null;
            return new ItemDurabilitySetter(valueElement.getAsInt());
        });

        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("base"))
                return null;

            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            if (attrElement == null || valueElement == null)
                return null;

            var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null)
            {
                Attributesetter.LOGGER.error("Failed to find attribute {} in entry {}", attrElement.getAsString(), id);
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

            // base/durability are handled by higher-priority builders
            if (opElement != null)
            {
                var opStr = opElement.getAsString();
                if (opStr.equalsIgnoreCase("base") || opStr.equalsIgnoreCase("durability"))
                    return null;
            }

            var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null)
            {
                Attributesetter.LOGGER.error("Failed to find attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }

            var slot = parseItemSlot(obj.get("slot"), id, selector);
            if (slot == null)
                return null;

            if (opElement == null)
                return new ItemAttributeModifierSetter(attr, AttributeModifier.Operation.ADDITION, value, slot, id);

            AttributeModifier.Operation op;
            try
            {
                op = AttributeModifier.Operation.valueOf(opElement.getAsString().toUpperCase());
            } catch (Exception ex)
            {
                Attributesetter.LOGGER.error("Failed to parse operation {} in entry {}", opElement.getAsString(), id);
                return null;
            }
            return new ItemAttributeModifierSetter(attr, op, value, slot, id);
        });

        // Conversion
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("amount");
            var rateElement = obj.get("rate");
            var opElement = obj.get("operation");
            var fromElement = obj.get("from");
            if (attrElement == null || opElement == null || fromElement == null)
                return null;
            if (!opElement.getAsString().equalsIgnoreCase("conversion"))
                return null;
            var toAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attrElement.getAsString()));
            var fromAttr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(fromElement.getAsString()));
            var amountConverted = valueElement != null ? valueElement.getAsFloat() : 1.0f;
            var conversionRate = rateElement != null ? rateElement.getAsFloat() : 1.0f;
            if (fromAttr == null)
            {
                Attributesetter.LOGGER.error("Failed to find source attribute {} in entry {}", fromElement.getAsString(), id);
                return null;
            }

            return new ItemAttributeConversionSetter(fromAttr, toAttr, amountConverted, conversionRate, id.toString());
        });
        // Dependency
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var multiplierElement = obj.get("multiplier");
            var opElement = obj.get("operation");
            var dependencyElement = obj.get("dependency");
            if (attrElement == null || opElement == null || multiplierElement == null)
                return null;
            if (!opElement.getAsString().equalsIgnoreCase("dependency"))
                return null;
            var attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(attrElement.getAsString()));
            var dependency = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(dependencyElement.getAsString()));
            var multiplier = multiplierElement != null ? multiplierElement.getAsFloat() : 1.0f;
            if (attr == null)
            {
                Attributesetter.LOGGER.error("Failed to find target attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }
            if (dependency == null)
            {
                Attributesetter.LOGGER.error("Failed to find dependency attribute {} in entry {}", dependencyElement.getAsString(), id);
                return null;
            }

            return new ItemAttributeDependencySetter(attr, dependency, multiplier, id.toString());
        });
    }

    private EquipmentSlot parseItemSlot(JsonElement slotElement, String id, ASSelector<ItemStack> selector)
    {
        if (slotElement == null)
        {
            IdItemSelector idSelector;
            if (selector instanceof IdItemSelector iis)
                idSelector = iis;
            else if (selector instanceof CompositeASSelector<ItemStack> cas)
            {
                IdItemSelector found = null;
                for (var selObj : cas.selectors)
                {
                    if (selObj instanceof IdItemSelector iis2)
                    {
                        found = iis2;
                        break;
                    }
                }
                idSelector = found;
            }
            else
                idSelector = null;

            if (idSelector != null)
            {
                var itemEntry = ForgeRegistries.ITEMS.getValue(idSelector.id);
                if (itemEntry instanceof ArmorItem ai)
                    return ai.getEquipmentSlot();
            }
            return EquipmentSlot.MAINHAND;
        }

        try {
            return EquipmentSlot.valueOf(slotElement.getAsString().toUpperCase());
        } catch (IllegalArgumentException e)
        {
            Attributesetter.LOGGER.error("Invalid slot: {} in entry {}", slotElement.getAsString(), id);
            return null;
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void tooltipEvent(ItemTooltipEvent e)
        {
            if (isApothic)
                return;
            var original = new ArrayList<>(e.getToolTip());
            try
            {
                var lines = e.getToolTip();
                Map<String, Map<String, Double>> blueAttributes = new HashMap<>();
                Map<String, Integer> slotIndexes = new HashMap<>();
                Map<String, Double> greenAttributes = new HashMap<>();
                String currentSlot = null;
                int i = 0;
                for (Iterator<Component> it = lines.iterator(); it.hasNext();)
                {
                    var line = it.next();
                    var content = line.getContents();
                    //Normal attr modifiers
                    if (content instanceof TranslatableContents ttc)
                    {
                        String dmgAttrName = "attribute.name.generic.attack_damage";
                        String spdAttrName = "attribute.name.generic.attack_speed";
                        if (ttc.getKey().startsWith("item.modifiers"))
                        {
                            currentSlot = ttc.getKey().substring(ttc.getKey().lastIndexOf('.')+1);
                            slotIndexes.put(currentSlot, i);
                        }
                        else if (ttc.getKey().startsWith("attribute.modifier.plus.0") && currentSlot != null)
                        {
                            if (!NumberUtils.isCreatable(ttc.getArgument(0).getString()))
                            {
                                i++;
                                continue;
                            }
                            var attrName = ((TranslatableContents)((MutableComponent)ttc.getArgument(1)).getContents()).getKey();
                            double value = Double.parseDouble(ttc.getArgument(0).getString());
                            if (greenAttributes.containsKey(attrName))
                            {
                                greenAttributes.put(attrName, greenAttributes.get(attrName) + value);
                                it.remove();
                            }
                            else if (attrName.equals(dmgAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, value+1);
                                it.remove();
                            }
                            else if (attrName.equals(spdAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, 4 + value);
                                it.remove();
                            }
                            else 
                            {
                                if (!blueAttributes.containsKey(currentSlot))
                                    blueAttributes.put(currentSlot, new HashMap<>());

                                blueAttributes.get(currentSlot).put(attrName, blueAttributes.get(currentSlot).getOrDefault(attrName, 0.0) + value);
                                it.remove();
                            }
                        }
                        else if (ttc.getKey().startsWith("attribute.modifier.take.0") && currentSlot != null)
                        {
                            if (!NumberUtils.isCreatable(ttc.getArgument(0).getString()))
                            {
                                i++;
                                continue;
                            }
                            var attrName = ((TranslatableContents)((MutableComponent)ttc.getArgument(1)).getContents()).getKey();
                            double value = Double.parseDouble(ttc.getArgument(0).getString());
                            if (greenAttributes.containsKey(attrName))
                            {
                                greenAttributes.put(attrName, greenAttributes.get(attrName) - value);
                                it.remove();
                            }
                            else if (attrName.equals(dmgAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, -value+1);
                                it.remove();
                            }
                            else if (attrName.equals(spdAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, 4 - value);
                                it.remove();
                            }
                            else
                            {
                                if (!blueAttributes.containsKey(currentSlot))
                                    blueAttributes.put(currentSlot, new HashMap<>());
                                blueAttributes.get(currentSlot).put(attrName, blueAttributes.get(currentSlot).getOrDefault(attrName, 0.0) - value);
                                it.remove();
                            }
                        }
                    }
                    else
                    {
                        for (var part : line.getSiblings())
                        {
                            if (part.getContents() instanceof TranslatableContents ttc && ttc.getKey().startsWith("attribute.modifier.equals.0"))
                            {
                                if (!NumberUtils.isCreatable(ttc.getArgument(0).getString()))
                                {
                                    i++;
                                    continue;
                                }
                                var attrName = ((TranslatableContents)((MutableComponent)ttc.getArgument(1)).getContents()).getKey();
                                greenAttributes.put(attrName, Double.parseDouble(ttc.getArgument(0).getString()));
                                it.remove();
                            }
                        }
                    }
                    i++;
                }
                for (var slotEntry : blueAttributes.entrySet())
                {
                    i = 0;
                    var slot = slotEntry.getKey();
                    for (var entry : slotEntry.getValue().entrySet())
                    {
                        var attrName = entry.getKey();
                        var value = entry.getValue();
                        if (value == 0)
                        {
                            i++;
                            continue;
                        }
                        var color = value > 0 ? ChatFormatting.BLUE : ChatFormatting.RED;
                        var line = Component.translatable(value > 0 ? "attribute.modifier.plus.0" : "attribute.modifier.take.0", Component.literal(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(value))).withStyle(color), Component.translatable(attrName).withStyle(color)).withStyle(color);
                        lines.add(slotIndexes.get(slot) + i + 1, line);
                        i++;
                    }
                }
                i = 0;
                for (var entry : greenAttributes.entrySet())
                {
                    var attrName = entry.getKey();
                    var value = entry.getValue();
                    var color = ChatFormatting.DARK_GREEN;
                    var line = Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0", Component.literal(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value)).withStyle(color), Component.translatable(attrName).withStyle(color)));
                    lines.add(slotIndexes.get("mainhand") + i + 1, line);
                    i++;
                }
            } catch (Exception ex)
            {
                e.getToolTip().clear();
                e.getToolTip().addAll(original);
            }

        }
    }
}
