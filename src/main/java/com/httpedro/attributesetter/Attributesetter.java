package com.httpedro.attributesetter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.httpedro.attributesetter.api.RemovalRegistry;
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
import com.httpedro.attributesetter.setters.entity.EntityRemoveSetter;
import com.httpedro.attributesetter.setters.item.ItemRemoveSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeBaseSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeConversionSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeDependencySetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeModifierSetter;
import com.httpedro.attributesetter.setters.item.ItemAttributeSetter;
import com.httpedro.attributesetter.setters.item.ItemDurabilitySetter;
import com.httpedro.attributesetter.selectors.entity.IsEnemyEntitySelector;
import com.httpedro.attributesetter.selectors.entity.MobCategoryEntitySelector;
import com.httpedro.attributesetter.selectors.item.IsFoodItemSelector;
import com.httpedro.attributesetter.selectors.item.HasDurabilityItemSelector;
import com.httpedro.attributesetter.selectors.item.IsEnchantedItemSelector;
import com.httpedro.attributesetter.selectors.item.IsPotionItemSelector;
import com.httpedro.attributesetter.selectors.attribute.IdAttributeSelector;
import com.httpedro.attributesetter.selectors.attribute.RegexAttributeSelector;
import com.httpedro.attributesetter.setters.item.ItemMaxStackSetter;
import com.httpedro.attributesetter.setters.item.food.FoodNutritionSetter;
import com.httpedro.attributesetter.setters.item.food.FoodNutritionMultiplierSetter;
import com.httpedro.attributesetter.setters.item.tooltip.ItemTooltipSetter;
import com.httpedro.attributesetter.setters.item.tooltip.ItemTooltipAddSetter;
import com.httpedro.attributesetter.setters.item.tooltip.ItemTooltipModifySetter;
import com.httpedro.attributesetter.setters.entity.CreeperExplosionPowerSetter;
import com.httpedro.attributesetter.setters.attribute.AttributeModifiersInjectionSetter;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.LogicalSide;
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
import java.util.List;
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
                        AttributeSetterAPI.applyGlobalItemSetters(false);
                    }
            );
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void datapackReload(AddReloadListenerEvent e)
    {
        e.addListener(dr);
        // Recipe removals run once the reload listener has parsed the entries, against the recipe manager this
        // reload is building - which is why we grab it here instead of going through the server.
        RemovalRegistry.setServerContext(e.getServerResources().getRecipeManager(), e.getRegistryAccess());
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent e)
    {
        // Don't hold on to a dead server's recipe manager: on a client, the next datapack sync could be a
        // different world entirely.
        RemovalRegistry.setServerContext(null, null);
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
            else if (entry instanceof ItemTooltipSetter)
            {
                // Handled in the client tooltip event.
                continue;
            }
            else if (entry instanceof com.httpedro.attributesetter.setters.item.ItemSetter is && is.isGlobal())
            {
                // Global item setters are applied once at reload, not per stack.
                continue;
            }
            else if (entry.shouldApply(stack))
                entry.apply(stack);
        }

    }

    /**
     * Applies every matching entry to the entity.
     * @return true when an entry asks for the entity to be deleted from the game, in which case nothing was
     *         applied and the caller is expected to keep it out of the world.
     */
    private boolean processEntity(LivingEntity le)
    {
        // A broad selector (isEnemy, a regex, a tag) must never be able to delete the player.
        boolean removable = !(le instanceof Player);
        if (removable && RemovalRegistry.isRemoved(le.getType()))
            return true;

        final var entries = AttributeSetterAPI.getEntriesFor(le);
        if (removable)
        {
            for (var entry : entries)
            {
                if (entry instanceof EntityRemoveSetter)
                    return true;
            }
        }

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
            return false;

        ((ASLivingEntity)le).as$setLoaded();

        for (var entry : entries)
        {
            entry.apply(le);
        }
        le.setHealth(le.getMaxHealth());
        return false;
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent e)
    {
        var world = e.getLevel();
        var entity = e.getEntity();
        if (world.isClientSide)
            return;

        // Removed items never make it into the world as a dropped stack, and removed entities never join a level -
        // which also takes care of the ones already saved in a chunk, since they are dropped on load.
        if (entity instanceof ItemEntity itemEntity)
        {
            if (RemovalRegistry.isRemoved(itemEntity.getItem().getItem()))
                e.setCanceled(true);
            return;
        }
        if (!(entity instanceof LivingEntity le))
            return;
        if (processEntity(le))
            e.setCanceled(true);
    }

    @SubscribeEvent
    public void onEntitySpawn(MobSpawnEvent.FinalizeSpawn e)
    {
        var world = e.getLevel();
        var entity = e.getEntity();
        if (world.isClientSide())
            return;

        if (processEntity(entity))
            e.setSpawnCancelled(true);
    }

    /**
     * Denies spawn attempts for removed entity types before the game builds a candidate, so removed mobs don't
     * keep eating the mob cap. Only type-resolvable removals get here; the rest are caught when they join.
     */
    @SubscribeEvent
    public void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck e)
    {
        if (RemovalRegistry.isRemoved(e.getEntityType()))
            e.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public void onEntityBred(BabyEntitySpawnEvent e)
    {
        if (e.getChild() == null)
            return;
        var world = e.getChild().level();
        if (world.isClientSide())
            return;
        if (processEntity(e.getChild()))
            e.setCanceled(true);
    }

    /**
     * Sweeps removed items out of player inventories, so copies that were already in the world when the datapack
     * changed disappear too. Throttled to once a second and skipped entirely when nothing is removed.
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e)
    {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER)
            return;
        if (!RemovalRegistry.hasRemovedItems())
            return;
        var player = e.player;
        if (player.tickCount % 20 != 0)
            return;

        boolean changed = sweepRemovedItems(player.getInventory());
        changed |= sweepRemovedItems(player.getEnderChestInventory());
        if (changed)
            player.containerMenu.broadcastChanges();
    }

    private static boolean sweepRemovedItems(Container container)
    {
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++)
        {
            var stack = container.getItem(i);
            if (stack.isEmpty() || !RemovalRegistry.isRemoved(stack.getItem()))
                continue;
            container.setItem(i, ItemStack.EMPTY);
            changed = true;
        }
        return changed;
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

        // Entity type/category selectors
        Map<String, MobCategory> mobCategoryMap = Map.of(
                "isMonster", MobCategory.MONSTER,
                "isCreature", MobCategory.CREATURE,
                "isMisc", MobCategory.MISC,
                "isWaterCreature", MobCategory.WATER_CREATURE
        );
        for (var entry : mobCategoryMap.entrySet())
        {
            AttributeSetterAPI.registerEntitySelectorBuilder(50, (str, fileName) -> {
                if (str.equals(entry.getKey()))
                    return new MobCategoryEntitySelector(entry.getValue());
                return null;
            });
        }
        AttributeSetterAPI.registerEntitySelectorBuilder(50, (str, fileName) -> {
            if (str.equals("isEnemy"))
                return new IsEnemyEntitySelector();
            return null;
        });

        // Item property selectors (component-equivalents on 1.20.1)
        AttributeSetterAPI.registerItemSelectorBuilder(60, (str, fileName) -> {
            switch (str)
            {
                case "isFood": return new IsFoodItemSelector();
                case "hasDurability": return new HasDurabilityItemSelector();
                case "isEnchanted": return new IsEnchantedItemSelector();
                case "isPotion": return new IsPotionItemSelector();
                default: return null;
            }
        });

        // Attribute selectors (ID / regex / inverted / composite) for the `attribute` datapack section
        AttributeSetterAPI.registerAttributeSelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
            String namespace = fileName;
            if (str.contains(":"))
            {
                var parts = str.split(":");
                namespace = parts[0];
                str = parts[1];
            }
            return new IdAttributeSelector(new ResourceLocation(namespace, str));
        });
        AttributeSetterAPI.registerAttributeSelectorBuilder(99, (str, fileName) -> {
            var prefix = "regex:";
            if (str.contains(prefix))
            {
                var regex = str.substring(str.indexOf(prefix) + prefix.length()).trim();
                return new RegexAttributeSelector(regex);
            }
            return null;
        });
        AttributeSetterAPI.registerAttributeSelectorBuilder(Integer.MAX_VALUE - 1, (str, fileName) -> {
            if (str.startsWith("!"))
            {
                var subSelector = AttributeSetterAPI.parseAttributeSelector(str.substring(1).trim(), fileName);
                if (subSelector != null)
                {
                    subSelector.inverted = true;
                    return subSelector;
                }
            }
            return null;
        });
        AttributeSetterAPI.registerAttributeSelectorBuilder(Integer.MAX_VALUE, (str, fileName) -> {
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
            var selectors = new ArrayList<ASSelector<Attribute>>();
            for (var part : parts)
            {
                var sel = AttributeSetterAPI.parseAttributeSelector(part.trim(), fileName);
                if (sel != null)
                    selectors.add(sel);
            }
            if (selectors.size() > 0)
                return new CompositeASSelector<Attribute>(selectors, mode);
            return null;
        });
    }

    private static boolean isRemoveOperation(com.google.gson.JsonObject obj)
    {
        var opElement = obj.get("operation");
        if (opElement == null)
            return false;
        var op = opElement.getAsString();
        return op.equalsIgnoreCase("remove") || op.equalsIgnoreCase("delete");
    }

    private static AttributeModifier.Operation parseOperation(String s)
    {
        switch (s.toUpperCase())
        {
            case "+":
            case "ADD":
            case "ADDITION":
            case "ADD_VALUE":
                return AttributeModifier.Operation.ADDITION;
            case "%":
            case "PERCENT":
            case "MULTIPLY_BASE":
            case "ADD_MULTIPLIED_BASE":
                return AttributeModifier.Operation.MULTIPLY_BASE;
            case "*":
            case "X":
            case "MULTIPLY_TOTAL":
            case "ADD_MULTIPLIED_TOTAL":
                return AttributeModifier.Operation.MULTIPLY_TOTAL;
            default:
                try {
                    return AttributeModifier.Operation.valueOf(s.toUpperCase());
                } catch (Exception ex) {
                    return null;
                }
        }
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
                    var op = parseOperation(opElement.getAsString());
                    if (op == null)
                    {
                        Attributesetter.LOGGER.error("Failed to parse operation {}", opElement.getAsString());
                        return null;
                    }
                    return new EntityAttributeModifierSetter(attr, op, value, id);
                }
            }
            return null;
        });

        // Removal: takes the entity out of the game entirely
        AttributeSetterAPI.registerEntitySetterBuilder(2, (obj, id, selector) -> {
            if (!isRemoveOperation(obj))
                return null;
            return new EntityRemoveSetter();
        });

        // Creeper explosion power
        AttributeSetterAPI.registerEntitySetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("creeper_explosion_power") && !op.equalsIgnoreCase("explosion_power"))
                return null;

            var powerElement = obj.get("power");
            if (powerElement != null)
                return new CreeperExplosionPowerSetter(powerElement.getAsInt());

            var multiplierElement = obj.get("multiplier");
            if (multiplierElement != null)
                return new CreeperExplosionPowerSetter(multiplierElement.getAsFloat());

            Attributesetter.LOGGER.error("Missing power or multiplier for creeper explosion power setter in entry {}", id);
            return null;
        });

        // Item setters: split by operation
        // - priority 2: removal (checked first)
        // - priority 1: durability/base
        // - priority 0: modifier (default)

        // Removal: takes the item out of the game entirely
        AttributeSetterAPI.registerItemSetterBuilder(2, (obj, id, selector) -> {
            if (!isRemoveOperation(obj))
                return null;
            return new ItemRemoveSetter();
        });

        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("durability"))
                return null;
            var valueElement = obj.get("value");
            var multiplierElement = obj.get("multiplier");
            if (valueElement != null)
                return new ItemDurabilitySetter(valueElement.getAsInt());
            if (multiplierElement != null)
                return new ItemDurabilitySetter(multiplierElement.getAsFloat());
            Attributesetter.LOGGER.error("Missing value or multiplier for durability setter in entry {}", id);
            return null;
        });

        // Max stack size
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("max_stack"))
                return null;
            var valueElement = obj.get("value");
            var multiplierElement = obj.get("multiplier");
            if (valueElement != null)
                return new ItemMaxStackSetter(valueElement.getAsInt());
            if (multiplierElement != null)
                return new ItemMaxStackSetter(multiplierElement.getAsFloat());
            Attributesetter.LOGGER.error("Missing value or multiplier for max stack size setter in entry {}", id);
            return null;
        });

        // Food
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("food"))
                return null;

            if (obj.has("remove") && obj.get("remove").getAsBoolean())
                return FoodNutritionSetter.remove();

            var nutritionElement = obj.get("nutrition");
            var saturationElement = obj.get("saturation");
            if (nutritionElement == null || saturationElement == null)
            {
                Attributesetter.LOGGER.error("Missing nutrition or saturation for food setter in entry {}", id);
                return null;
            }
            int nutrition = nutritionElement.getAsInt();
            float saturation = saturationElement.getAsFloat();
            boolean canAlwaysEat = obj.has("can_always_eat") && obj.get("can_always_eat").getAsBoolean();
            boolean fastFood = obj.has("fast_food") && obj.get("fast_food").getAsBoolean();

            Integer eatTicks = null;
            var eatSecondsElement = obj.get("eat_seconds");
            if (eatSecondsElement != null)
                eatTicks = Math.round(eatSecondsElement.getAsFloat() * 20);

            ItemStack convertsTo = null;
            var convertsToElement = obj.get("converts_to");
            if (convertsToElement != null)
            {
                var convItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(convertsToElement.getAsString()));
                if (convItem != null && convItem != Items.AIR)
                    convertsTo = new ItemStack(convItem);
                else
                    Attributesetter.LOGGER.error("Failed to parse converts_to item {} for food setter in entry {}", convertsToElement.getAsString(), id);
            }

            List<Pair<MobEffectInstance, Float>> effects = new ArrayList<>();
            var effectsElement = obj.get("effects");
            if (effectsElement != null && effectsElement.isJsonArray())
            {
                for (var effectElem : effectsElement.getAsJsonArray())
                {
                    if (!effectElem.isJsonObject())
                    {
                        Attributesetter.LOGGER.error("Invalid effect entry in effects array for food setter in entry {}", id);
                        continue;
                    }
                    var effectObj = effectElem.getAsJsonObject();
                    var effectTypeElem = effectObj.get("effect");
                    var durationElem = effectObj.get("duration");
                    if (effectTypeElem == null || durationElem == null)
                    {
                        Attributesetter.LOGGER.error("Missing effect or duration for food effect in entry {}", id);
                        continue;
                    }
                    var effectRes = new ResourceLocation(effectTypeElem.getAsString());
                    MobEffect effectType = ForgeRegistries.MOB_EFFECTS.getValue(effectRes);
                    if (effectType == null)
                    {
                        Attributesetter.LOGGER.error("Failed to find mob effect {} for food setter in entry {}", effectRes, id);
                        continue;
                    }
                    int duration = durationElem.getAsInt();
                    int amplifier = effectObj.has("amplifier") ? effectObj.get("amplifier").getAsInt() : 0;
                    float chance = effectObj.has("chance") ? effectObj.get("chance").getAsFloat() : 1.0f;
                    effects.add(Pair.of(new MobEffectInstance(effectType, duration, amplifier), chance));
                }
            }

            return new FoodNutritionSetter(nutrition, saturation, canAlwaysEat, fastFood, eatTicks, convertsTo, effects);
        });

        // Food modify
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("food_modify"))
                return null;

            var saturationElement = obj.get("saturation_multiplier");
            var nutritionElement = obj.get("nutrition_multiplier");
            var eatSecondsElement = obj.get("eat_seconds_mult");
            var saturationOffElement = obj.get("saturation_offset");
            var nutritionOffElement = obj.get("nutrition_offset");
            var eatSecondsOffElement = obj.get("eat_seconds_offset");

            float saturationMultiplier = saturationElement != null ? saturationElement.getAsFloat() : 1.0f;
            float nutritionMultiplier = nutritionElement != null ? nutritionElement.getAsFloat() : 1.0f;
            float eatSecondsMultiplier = eatSecondsElement != null ? eatSecondsElement.getAsFloat() : 1.0f;
            float saturationOffset = saturationOffElement != null ? saturationOffElement.getAsFloat() : 0;
            int nutritionOffset = nutritionOffElement != null ? nutritionOffElement.getAsInt() : 0;
            float eatSecondsOffset = eatSecondsOffElement != null ? eatSecondsOffElement.getAsFloat() : 0;

            return new FoodNutritionMultiplierSetter(nutritionMultiplier, saturationMultiplier, eatSecondsMultiplier, nutritionOffset, saturationOffset, eatSecondsOffset);
        });

        // Tooltip: add lines
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("tooltip_add") && !op.equalsIgnoreCase("tooltipadd") && !op.equalsIgnoreCase("tooltip"))
                return null;

            var tooltipElement = obj.get("tooltip");
            if (tooltipElement == null)
                tooltipElement = obj.get("components");
            if (tooltipElement == null)
                return null;

            List<Component> comps = parseTooltipComponents(tooltipElement, id);
            if (comps.isEmpty())
                return null;
            return new ItemTooltipAddSetter(comps.toArray(new Component[0]));
        });

        // Tooltip: modify (insert/replace/remove)
        AttributeSetterAPI.registerItemSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            if (!opElement.getAsString().equalsIgnoreCase("tooltip_modify") && !opElement.getAsString().equalsIgnoreCase("tooltipmodify"))
                return null;

            int index = obj.has("index") ? obj.get("index").getAsInt() : 0;
            ItemTooltipModifySetter.Type type = ItemTooltipModifySetter.Type.INSERT;
            var typeElement = obj.get("type");
            if (typeElement != null)
            {
                try {
                    type = ItemTooltipModifySetter.Type.valueOf(typeElement.getAsString().toUpperCase());
                } catch (Exception ex) {
                    Attributesetter.LOGGER.error("Invalid tooltip modify type {} in entry {}", typeElement.getAsString(), id);
                    return null;
                }
            }

            var componentsElement = obj.get("components");
            if (componentsElement == null)
                componentsElement = obj.get("tooltip");
            List<Component> comps = componentsElement != null ? parseTooltipComponents(componentsElement, id) : new ArrayList<>();

            if (type != ItemTooltipModifySetter.Type.REMOVE && comps.isEmpty())
                return null;

            return new ItemTooltipModifySetter(index, type, comps.toArray(new Component[0]));
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

            AttributeModifier.Operation op = parseOperation(opElement.getAsString());
            if (op == null)
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

        // Attribute injection / merging
        AttributeSetterAPI.registerAttributeSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var opStr = opElement.getAsString();
            if (!opStr.equalsIgnoreCase("inject") && !opStr.equalsIgnoreCase("injection") && !opStr.equalsIgnoreCase("attribute_injection"))
                return null;

            var sourceElement = obj.get("source");
            if (sourceElement == null)
            {
                Attributesetter.LOGGER.error("Missing source for attribute injection in entry {}", id);
                return null;
            }
            var source = new ResourceLocation(sourceElement.getAsString());
            float multiplier = obj.has("multiplier") ? obj.get("multiplier").getAsFloat() : 1.0f;

            AttributeModifiersInjectionSetter.InjectionType type = AttributeModifiersInjectionSetter.InjectionType.INJECT;
            var typeElement = obj.get("type");
            if (typeElement != null)
            {
                try {
                    type = AttributeModifiersInjectionSetter.InjectionType.valueOf(typeElement.getAsString().toUpperCase());
                } catch (Exception ex) {
                    Attributesetter.LOGGER.error("Failed to parse injection type {} in entry {}", typeElement.getAsString(), id);
                    return null;
                }
            }

            List<AttributeModifier.Operation> operations = null;
            var operationsElement = obj.get("operations");
            if (operationsElement != null)
            {
                operations = new ArrayList<>();
                if (operationsElement.isJsonArray())
                {
                    for (var opElem : operationsElement.getAsJsonArray())
                    {
                        if (!opElem.isJsonPrimitive())
                            continue;
                        var parsedOp = parseOperation(opElem.getAsString());
                        if (parsedOp == null)
                        {
                            Attributesetter.LOGGER.error("Failed to parse operation {} in entry {}", opElem.getAsString(), id);
                            return null;
                        }
                        operations.add(parsedOp);
                    }
                }
                else if (operationsElement.isJsonPrimitive())
                {
                    var parsedOp = parseOperation(operationsElement.getAsString());
                    if (parsedOp == null)
                    {
                        Attributesetter.LOGGER.error("Failed to parse operation {} in entry {}", operationsElement.getAsString(), id);
                        return null;
                    }
                    operations.add(parsedOp);
                }
                if (operations.isEmpty())
                {
                    Attributesetter.LOGGER.error("No valid operations provided for attribute injection in entry {}", id);
                    return null;
                }
            }

            return new AttributeModifiersInjectionSetter(source, operations, type, multiplier);
        });
    }

    private List<Component> parseTooltipComponents(JsonElement element, String id)
    {
        List<Component> comps = new ArrayList<>();
        if (element.isJsonArray())
        {
            for (var elem : element.getAsJsonArray())
            {
                try {
                    if (elem.isJsonPrimitive())
                        comps.add(Component.literal(elem.getAsString()));
                    else
                        comps.add(Component.Serializer.fromJson(elem.toString()));
                } catch (Exception ex) {
                    Attributesetter.LOGGER.error("Failed to parse tooltip component in entry {}", id, ex);
                }
            }
        }
        else if (element.isJsonPrimitive())
        {
            comps.add(Component.literal(element.getAsString()));
        }
        else
        {
            try {
                comps.add(Component.Serializer.fromJson(element.toString()));
            } catch (Exception ex) {
                Attributesetter.LOGGER.error("Failed to parse tooltip component in entry {}", id, ex);
            }
        }
        comps.removeIf(c -> c == null);
        return comps;
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
            // Tooltip setters run regardless of Apothic (which only affects the attribute-merge display below).
            for (var setter : AttributeSetterAPI.getEntriesFor(e.getItemStack()))
            {
                if (setter instanceof ItemTooltipSetter tts)
                    tts.apply(e);
            }

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
