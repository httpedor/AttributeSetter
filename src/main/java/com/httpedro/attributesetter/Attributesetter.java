package com.httpedro.attributesetter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.httpedro.attributesetter.compat.CuriosCompat;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Attributesetter.MODID)
public class Attributesetter {
    public static final UUID DEFAULT_UUID = UUID.fromString("21ef99f1-c77a-42cf-ba8f-a59cf69ce7a6");
    public static final UUID BASE_UUID = UUID.fromString("b697bf19-6a3a-4baf-89ce-5d4a3422a3a4");

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
            MinecraftForge.EVENT_BUS.register(new CuriosCompat());
    }

    public void commonSetup(FMLCommonSetupEvent e)
    {
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
                        for (Object obj : map.entrySet())
                        {
                            var entry = (Map.Entry<ResourceLocation, JsonElement>) obj;
                            dr.addEntry(entry.getKey(), entry.getValue());
                        }
                    }
            );
        });
    }

    @SubscribeEvent
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
        var slot = e.getSlotType();

        var item = stack.getItem();
        var id = ForgeRegistries.ITEMS.getKey(item);
        for (var entry : AttributeSetterAPI.BASE_TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.is(TagKey.create(Registries.ITEM, entry.getKey()))
                    && entry.getValue().containsKey(slot))
            {
                for (var modEntry : entry.getValue().get(slot).entrySet())
                {
                    e.removeAttribute(modEntry.getKey());
                    e.addModifier(modEntry.getKey(), new AttributeModifier(BASE_UUID, "ASMod", modEntry.getValue(), AttributeModifier.Operation.ADDITION));
                }
            }
        }
        for (var entry : AttributeSetterAPI.BASE_ITEM_MODIFIERS.entrySet())
        {
            if (entry.getKey().equals(id) && entry.getValue().containsKey(slot))
            {
                for (var modEntry : entry.getValue().get(slot).entrySet())
                {
                    e.removeAttribute(modEntry.getKey());
                    e.addModifier(modEntry.getKey(), new AttributeModifier(BASE_UUID, "ASMod", modEntry.getValue(), AttributeModifier.Operation.ADDITION));
                }
            }
        }
        for (var entry : AttributeSetterAPI.TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.is(TagKey.create(Registries.ITEM, entry.getKey())) && entry.getValue().containsKey(slot))
            {
                for (var modEntry : entry.getValue().get(slot).entrySet())
                {
                    e.addModifier(modEntry.getKey(), modEntry.getValue());
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
                    e.addModifier(entry.getKey(), entry.getValue());
                }
            }
        }

    }

    private void processEntity(LivingEntity le)
    {
        if (((ASLivingEntity)le).as$isLoaded())
            return;

        ((ASLivingEntity)le).as$setLoaded();
        var entityType = ForgeRegistries.ENTITY_TYPES.getKey(le.getType());
        var id = new ResourceLocation(entityType.getNamespace(), entityType.getPath());
        for (var entry : AttributeSetterAPI.BASE_TAG_MODIFIERS.entrySet())
        {
            if (le.getType().is(TagKey.create(Registries.ENTITY_TYPE, entry.getKey())))
            {
                for (var modEntry : entry.getValue().entrySet())
                {
                    var attrInstance = le.getAttribute(modEntry.getKey());
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
                var attrInstance = le.getAttribute(entry.getKey());
                if (attrInstance != null)
                    attrInstance.setBaseValue(entry.getValue());
            }
        }

        for (var entry : AttributeSetterAPI.TAG_MODIFIERS.entrySet())
        {
            if (le.getType().is(TagKey.create(Registries.ENTITY_TYPE, entry.getKey())))
            {
                for (var modEntry : entry.getValue().entrySet())
                {
                    var attrInstance = le.getAttribute(modEntry.getKey());
                    if (attrInstance != null)
                        attrInstance.addPermanentModifier(modEntry.getValue());
                }
            }
        }

        var modifiers = AttributeSetterAPI.ENTITY_MODIFIERS.getOrDefault(id, null);
        if (modifiers != null)
        {
            for (var entry : modifiers.entrySet())
            {
                var attrInstance = le.getAttribute(entry.getKey());
                if (attrInstance != null)
                    attrInstance.addPermanentModifier(entry.getValue());
            }
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

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void tooltipEvent(ItemTooltipEvent e)
        {
            var lines = e.getToolTip();
            Map<String, Double> greenAttributes = new HashMap<>();
            String currentSlot = null;
            int mainhandSlotIndex = -1;
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
                        if (currentSlot.equals("mainhand"))
                            mainhandSlotIndex = i;
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
                    }
                }
                //Green attr
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
            i = 0;
            for (var entry : greenAttributes.entrySet())
            {
                var attrName = entry.getKey();
                var value = entry.getValue();
                var line = Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0", Component.literal(ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(value)).withStyle(ChatFormatting.DARK_GREEN), Component.translatable(attrName).withStyle(ChatFormatting.DARK_GREEN)));
                lines.add(mainhandSlotIndex + i + 1, line);
                i++;
            }

        }
    }
}
