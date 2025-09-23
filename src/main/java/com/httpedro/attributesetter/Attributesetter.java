package com.httpedro.attributesetter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.httpedro.attributesetter.compat.CuriosCompat;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.world.item.component.ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Attributesetter.MODID)
public class Attributesetter {
    public static final UUID DEFAULT_UUID = UUID.fromString("21ef99f1-c77a-42cf-ba8f-a59cf69ce7a6");
    public static final UUID BASE_UUID = UUID.fromString("b697bf19-6a3a-4baf-89ce-5d4a3422a3a4");

    // Define mod id in a common place for everything to reference
    static final DataReloader dr = new DataReloader();
    public static final String MODID = "attributesetter";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";

    public Attributesetter(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::registerPayload);
        if (ModList.get().isLoaded("curios"))
            NeoForge.EVENT_BUS.register(new CuriosCompat());
    }

    public void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1"); // Protocol version
        registrar.configurationToClient(
                DataLoginSyncPacket.TYPE,
                DataLoginSyncPacket.DECODER,
                DataLoginSyncPacket.HANDLER
        );
    }

    @SubscribeEvent
    public void datapackReload(AddReloadListenerEvent e)
    {
        e.addListener(dr);
    }

    @SubscribeEvent
    public void syncData(OnDatapackSyncEvent e)
    {
        /*for (var p : e.getPlayerList().getPlayers())
        {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), dr.entries);
        }*/
    }

    @SubscribeEvent
    public void onItemAttribute(ItemAttributeModifierEvent e)
    {
        var stack = e.getItemStack();

        var item = stack.getItem();
        var id = BuiltInRegistries.ITEM.getKey(item);
        for (var entry : AttributeSetterAPI.BASE_TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.is(TagKey.create(Registries.ITEM, entry.getKey())))
            {
                applyBases(e, entry);
            }
        }
        for (var entry : AttributeSetterAPI.BASE_ITEM_MODIFIERS.entrySet())
        {
            if (entry.getKey().equals(id))
            {
                applyBases(e, entry);
            }
        }
        for (var entry : AttributeSetterAPI.TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.is(TagKey.create(Registries.ITEM, entry.getKey())))
            {
                for (var slot : entry.getValue().keySet())
                {
                    for (var modEntry : entry.getValue().get(slot).entrySet())
                    {
                        e.addModifier(modEntry.getKey(), modEntry.getValue(), slot);
                    }
                }
            }
        }

        var modifiers = AttributeSetterAPI.ITEM_MODIFIERS.getOrDefault(id, null);
        if (modifiers != null)
        {
            for (var slot : modifiers.keySet())
            {
                for (var mod : modifiers.get(slot).entrySet())
                {
                    e.addModifier(mod.getKey(), mod.getValue(), slot);
                }
            }
        }

    }

    private void applyBases(ItemAttributeModifierEvent e, Map.Entry<ResourceLocation, Map<EquipmentSlotGroup, Map<Holder<Attribute>, Double>>> entry) {
        for (var slot : entry.getValue().keySet())
        {
            for (var modEntry : entry.getValue().get(slot).entrySet())
            {
                e.removeAllModifiersFor(modEntry.getKey());
                e.addModifier(modEntry.getKey(), new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MODID, "base_mod"), modEntry.getValue(), AttributeModifier.Operation.ADD_VALUE), slot);
            }
        }
    }

    private void processEntity(LivingEntity le)
    {
        final var entityType = BuiltInRegistries.ENTITY_TYPE.getKey(le.getType());
        final var id = ResourceLocation.fromNamespaceAndPath(entityType.getNamespace(), entityType.getPath());
        Runnable processBase = () ->
        {
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
        };
        if (le.getType() == EntityType.PLAYER)
            processBase.run();
        if (((ASLivingEntity)le).as$isLoaded())
            return;

        ((ASLivingEntity)le).as$setLoaded();
        processBase.run();

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
    public void onEntitySpawn(FinalizeSpawnEvent e)
    {
        var world = e.getLevel();
        var entity = e.getEntity();
        if (world.isClientSide())
            return;

        processEntity(entity);
    }

    /*// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void tooltipEvent(ItemTooltipEvent e)
        {
            ItemStack stack = e.getItemStack();
            var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (modifiers == null) return;

            Map<Holder<Attribute>, Double> merged = new HashMap<>();

            for (var entry : modifiers.modifiers()) {
                AttributeModifier mod = entry.modifier();
                Holder<Attribute> attr = entry.attribute();
                merged.merge(attr, mod.amount(), Double::sum);
            }

            //Remove mainhand attributes
            int mainhandIndex = -1;
            int nextModIndex = -1;
            int i = 0;
            for (var line : e.getToolTip())
            {
                if (line.getContents() instanceof TranslatableContents ttc && ttc.getKey().equals("item.modifiers.mainhand"))
                    mainhandIndex = i;
                i++;
            }
            if (mainhandIndex != -1)
            {
                for (i = mainhandIndex; i < e.getToolTip().size(); ++i)
                {
                    var line = e.getToolTip().get(i);
                    if (line.getContents() instanceof PlainTextContents.LiteralContents literal && literal.text())
                        nextModIndex = i;
                }
            }


            // Add merged lines back
            for (var entry : merged.entrySet()) {
                double value = entry.getValue();
                Component attrName = Component.translatable(entry.getKey().value().getDescriptionId())
                        .withStyle(ChatFormatting.DARK_GREEN);

                Component line = Component.translatable(
                        "attribute.modifier.equals.0",
                        Component.literal(ATTRIBUTE_MODIFIER_FORMAT.format(value))
                                .withStyle(ChatFormatting.DARK_GREEN),
                        attrName
                );

                e.getToolTip().add(line);
            }
        }
    }*/
}
