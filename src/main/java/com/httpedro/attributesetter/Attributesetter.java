package com.httpedro.attributesetter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.httpedro.attributesetter.network.NetworkHandler;
import com.httpedro.attributesetter.network.SyncTargetTypesPayload;
import com.httpedro.attributesetter.selectors.entity.IsEnemySelector;
import com.httpedro.attributesetter.selectors.entity.IsMobCategorySelector;
import com.httpedro.attributesetter.setters.item.ItemMaxStackSetter;
import com.httpedro.attributesetter.setters.item.ItemRemoveSetter;
import com.httpedro.attributesetter.setters.itemstack.ItemStackRemoveSetter;
import com.httpedro.attributesetter.setters.entity.EntityRemoveSetter;
import com.httpedro.attributesetter.setters.block.BlockExplosionResistanceSetter;
import com.httpedro.attributesetter.setters.block.BlockHardnessSetter;
import com.httpedro.attributesetter.setters.block.BlockMiningSpeedSetter;
import com.httpedro.attributesetter.setters.block.BlockRemoveSetter;
import com.httpedro.attributesetter.setters.block.BlockReplaceSetter;
import com.httpedro.attributesetter.api.RemovalRegistry;
import com.httpedro.attributesetter.api.UniqueRegistry;
import com.httpedro.attributesetter.setters.item.ItemUniqueSetter;
import com.httpedro.attributesetter.setters.itemstack.ItemStackUniqueSetter;
import com.httpedro.attributesetter.setters.entity.EntityUniqueSetter;
import com.httpedro.attributesetter.setters.attribute.AttributeModifiersInjectionSetter;
import com.httpedro.attributesetter.targettypes.*;
import com.httpedro.attributesetter.targettypes.interfaces.IDataComponentHolderTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IIdentifiableTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.INBTSerializableTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IRegistryAssociatedTargetType;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.httpedro.attributesetter.api.AttributeSetterAPI;
import com.httpedro.attributesetter.compat.curios.CuriosCompat;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.CompositeASSelector;
import com.httpedro.attributesetter.selectors.IdSelector;
import com.httpedro.attributesetter.selectors.NbtSelector;
import com.httpedro.attributesetter.selectors.RegexSelector;
import com.httpedro.attributesetter.selectors.TagSelector;
import com.httpedro.attributesetter.selectors.HasComponentSelector;
import com.httpedro.attributesetter.setters.entity.EntityAttributeModifierSetter;
import com.httpedro.attributesetter.setters.entity.EntityAttributeSetter;
import com.httpedro.attributesetter.setters.entity.CreeperExplosionPowerSetter;
import com.httpedro.attributesetter.setters.itemstack.attribute.ItemAttributeBaseSetter;
import com.httpedro.attributesetter.setters.itemstack.attribute.ItemAttributeConversionSetter;
import com.httpedro.attributesetter.setters.itemstack.attribute.ItemAttributeDependencySetter;
import com.httpedro.attributesetter.setters.itemstack.attribute.ItemAttributeModifierSetter;
import com.httpedro.attributesetter.setters.itemstack.tooltip.ItemTooltipAddSetter;
import com.httpedro.attributesetter.setters.itemstack.tooltip.ItemTooltipModifySetter;
import net.minecraft.network.chat.Component;
import com.httpedro.attributesetter.setters.item.ItemDurabilitySetter;
import com.httpedro.attributesetter.setters.item.food.FoodNutritionMultiplierSetter;
import com.httpedro.attributesetter.setters.item.food.FoodNutritionSetter;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.commands.arguments.item.ItemParser.ItemResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Attributesetter.MODID)
public class Attributesetter {
    public static boolean isApothic = false;

    // Define mod id in a common place for everything to reference
    public static final DataReloader dr = new DataReloader();
    public static RegistryAccess ra = null;
    public static final String MODID = "attributesetter";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public Attributesetter(IEventBus modEventBus, ModContainer modContainer) {
        TargetTypes.bootstrap();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);

        NeoForge.EVENT_BUS.register(this);
        if (FMLEnvironment.dist.isClient())
            NeoForge.EVENT_BUS.addListener(AttributeSetterClient::mergeTooltips);

        if (ModList.get().isLoaded("curios"))
        {
            var compat = new CuriosCompat();
            compat.bootstrap();
        }
    }

    public void commonSetup(FMLCommonSetupEvent e)
    {
        setupSelectors();
        setupSetters();
        isApothic = ModList.get().isLoaded("attributeslib");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void datapackReload(AddReloadListenerEvent e)
    {
        ra = e.getRegistryAccess();
        // Recipe removals run once the reload listener has parsed the entries, against the recipe manager this
        // reload is building - which is why we grab it here instead of going through the server.
        RemovalRegistry.setServerContext(e.getServerResources().getRecipeManager(), e.getRegistryAccess());
        e.addListener(dr);
    }

    @SubscribeEvent
    public void serverStarting(ServerStartingEvent e)
    {
        // Give the unique registry a handle to the running server so it can reach the per-save counts.
        UniqueRegistry.setServer(e.getServer());
    }

    @SubscribeEvent
    public void serverStopped(ServerStoppedEvent e)
    {
        // Don't hold on to a dead server's recipe manager: on a client, the next world could be a different one.
        RemovalRegistry.setServerContext(null, null);
        UniqueRegistry.setServer(null);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e)
    {
        Commands.register(e.getDispatcher());
    }

    /**
     * Counts crafted unique items and enforces their cap: if fewer than the crafted amount are still allowed, the
     * result stack is shrunk to what's permitted (0 = the player gets nothing).
     */
    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent e)
    {
        if (!UniqueRegistry.hasItemRules())
            return;
        var player = e.getEntity();
        if (player.level().isClientSide)
            return;
        var crafted = e.getCrafting();
        if (crafted.isEmpty())
            return;
        int allowed = UniqueRegistry.tryConsumeItem(crafted.getItem(), crafted.getCount(), player.level());
        if (allowed < crafted.getCount())
            crafted.setCount(Math.max(0, allowed));
    }

    /**
     * Applies every matching entry to the entity.
     * @return true when an entry asks for the entity to be deleted from the game, in which case nothing was
     *         applied and the caller is expected to keep it out of the world.
     */
    /** NBT flag stored on an entity once it has been counted against a unique cap, so chunk reloads don't recount. */
    public static final String UNIQUE_COUNTED_TAG = "attributesetter:unique_counted";

    public static boolean processEntity(LivingEntity le)
    {
        return processEntity(le, true);
    }

    /**
     * @param newSpawn true when this is a genuine new spawn that should count against (and be gated by) unique
     *                 caps; false for entities loaded from disk, which are grandfathered in (tagged as counted
     *                 without incrementing, so a cap added after they existed doesn't cull or recount them).
     */
    public static boolean processEntity(LivingEntity le, boolean newSpawn)
    {
        // A broad selector (isEnemy, a regex, a tag) must never be able to delete the player.
        boolean removable = !(le instanceof Player);
        if (removable && RemovalRegistry.isRemoved(le.getType()))
            return true;

        if (removable && UniqueRegistry.hasEntityRules() && processUniqueEntity(le, newSpawn))
            return true;

        final var entries = TargetTypes.ENTITY.getGenericEntriesFor(le);
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

    /**
     * Gates one entity against its unique cap. Idempotent per entity instance via the {@link #UNIQUE_COUNTED_TAG}
     * persistent flag, so an entity that already counts is always allowed and never counted twice (chunk reloads,
     * or the PositionCheck / JoinLevel double fire of a single spawn).
     *
     * @return true when the entity is over the cap and must be kept out of the world.
     */
    private static boolean processUniqueEntity(LivingEntity le, boolean newSpawn)
    {
        var rule = UniqueRegistry.getEntityRule(le.getType());
        if (rule == null)
            return false;

        var pdata = le.getPersistentData();
        if (pdata.getBoolean(UNIQUE_COUNTED_TAG))
            return false;

        // Entities that were already in the save when the cap appeared are grandfathered: tag them so they aren't
        // culled or counted, but leave the counter alone.
        if (!newSpawn)
        {
            pdata.putBoolean(UNIQUE_COUNTED_TAG, true);
            return false;
        }

        if (UniqueRegistry.isEntityBlocked(le.getType()))
            return true;

        UniqueRegistry.consumeEntity(le.getType(), le.level());
        pdata.putBoolean(UNIQUE_COUNTED_TAG, true);
        if (le instanceof Mob mob)
            mob.setPersistenceRequired();
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
        // Entities loaded from disk already existed in this save, so they must not count against (or be culled by)
        // a unique cap that was added later - only genuine new spawns do.
        if (processEntity(le, !e.loadedFromDisk()))
        {
            e.setCanceled(true);
            return;
        }

        // Sync TargetType data to players when they join the server
        if (entity instanceof ServerPlayer player) {
            SyncTargetTypesPayload payload = new SyncTargetTypesPayload(new java.util.HashMap<>(dr.entries));
            player.connection.send(payload);
        }
    }

    @SubscribeEvent
    public void onEntitySpawn(PositionCheck e)
    {
        var world = e.getLevel();
        var entity = e.getEntity();
        if (world.isClientSide())
            return;

        if (processEntity(entity))
            e.setResult(PositionCheck.Result.FAIL);
    }

    /**
     * Denies spawn attempts for removed entity types before the game builds a candidate, so removed mobs don't
     * keep eating the mob cap. Only type-resolvable removals get here; the rest are caught when they join.
     */
    @SubscribeEvent
    public void onSpawnPlacementCheck(SpawnPlacementCheck e)
    {
        if (RemovalRegistry.isRemoved(e.getEntityType()))
            e.setResult(SpawnPlacementCheck.Result.FAIL);
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
    public void onPlayerTick(PlayerTickEvent.Post e)
    {
        var player = e.getEntity();
        if (player.level().isClientSide)
            return;
        if (!RemovalRegistry.hasRemovedItems() || player.tickCount % 20 != 0)
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
    @SuppressWarnings("unchecked")
	private void setupSelectors()
    {

        Map<String, DataComponentType<?>> components = Map.of(
                "isFood", DataComponents.FOOD,
                "hasDurability", DataComponents.MAX_DAMAGE,
                "isEnchanted", DataComponents.ENCHANTMENTS,
                "isPotion", DataComponents.POTION_CONTENTS
        );
        for (var targetType : AttributeSetterAPI.getAllTargetTypes())
        {
            // ID Selector
            if (targetType instanceof IIdentifiableTargetType itt)
            {
                targetType.registerSelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
                    String namespace = fileName;
                    if (str.contains(":"))
                    {
                        var parts = str.split(":");
                        namespace = parts[0];
                        str = parts[1];
                    }
                    var res = ResourceLocation.fromNamespaceAndPath(namespace, str);
                    return new IdSelector(res, itt::getId);
                });
                // Regex Selector
                targetType.registerSelectorBuilder(99, (str, fileName) -> {
                    var prefix = "regex:";
                    if (str.contains(prefix))
                    {
                        var regex = str.substring(str.indexOf(prefix) + prefix.length()).trim();
                        return new RegexSelector(regex, (v) -> itt.getId(v).toString());
                    }
                    return null;
                });
            }
            // Inverted selector
            targetType.registerSelectorBuilder(Integer.MAX_VALUE - 1, (String str, String fileName) -> {
                if (str.startsWith("!"))
                {
                    var actualStr = str.substring(1).trim();
                    var subSelector = targetType.parseSelector(actualStr, fileName);
                    if (subSelector != null)
                    {
                        subSelector.inverted = true;
                        return (ASSelector)subSelector;
                    }
                }
                return null;
            });
            // Composite Selector
            targetType.registerSelectorBuilder(Integer.MAX_VALUE, (String str, String fileName) -> {
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
                var selectors = new ArrayList<ASSelector<?>>();
                for (var part : parts)
                {
                    var sel = targetType.parseSelector(part.trim(), fileName);
                    if (sel != null)
                        selectors.add(sel);
                }
                if (selectors.size() > 0)
                    return new CompositeASSelector(selectors, mode);

                return null;
            });
            if (targetType instanceof RegistryTargetType rtt)
            {
                targetType.registerSelectorBuilder(50, (str, fileName) -> {
                    if (str.startsWith("#"))
                    {
                        var tag = str.substring(1);
                        var tagRes = ResourceLocation.parse(tag);
                        return new TagSelector<>(tagRes, rtt.getRegistry(), rtt::getSingleton);
                    }
                    return null;
                });
            }
            else if (targetType instanceof IRegistryAssociatedTargetType iratt && targetType instanceof SingletonTargetType<?>)
            {
                targetType.registerSelectorBuilder(50, (str, fileName) -> {
                    if (str.startsWith("#"))
                    {
                        var tag = str.substring(1);
                        var tagRes = ResourceLocation.parse(tag);
                        return new TagSelector<>(tagRes, iratt.getRegistry(), (v) -> v);
                    }
                    return null;
                });
            }
            if (targetType instanceof INBTSerializableTargetType istt)
            {
                targetType.registerSelectorBuilder(100, (str, fileName) -> {
                    var nbtStartIndex = str.indexOf('{');
                    var nbtEndIndex = str.lastIndexOf('}');
                    if (nbtStartIndex != -1 && nbtEndIndex != -1 && nbtEndIndex > nbtStartIndex)
                    {
                        String beforePart = str.substring(0, nbtStartIndex).trim();
                        String nbtPart = str.substring(nbtStartIndex, nbtEndIndex + 1);
                        ASSelector<?> beforeSelector;
                        if (beforePart.isEmpty())
                            beforeSelector = null;
                        else
                            beforeSelector = targetType.parseSelector(beforePart, fileName);
                        if (beforeSelector == null && !beforePart.isEmpty())
                            return null;
                        try {
                            if (beforeSelector != null)
                            {
                                return new CompositeASSelector<>(new ASSelector[] {
                                        beforeSelector,
                                        new NbtSelector<>(nbtPart, istt.getSerializer())
                                }, CompositeASSelector.Mode.AND);
                            }
                            else
                                return new NbtSelector<>(nbtPart, istt.getSerializer());
                        } catch (Exception ex)
                        {
                            Attributesetter.LOGGER.error("Failed to parse NBT selector part '{}'", nbtPart, ex);
                            return null;
                        }
                    }
                    return null;
                });
            }
            if (targetType instanceof IDataComponentHolderTargetType idchtt)
            {
                for (var entry : components.entrySet())
                {

                    final String compName = entry.getKey();
                    final DataComponentType<?> compType = entry.getValue();
                    targetType.registerSelectorBuilder(60, (str, fileName) -> {
                        if (str.equals(compName))
                            return new HasComponentSelector(compType, idchtt::getDataComponentMap);
                        return null;
                    });
                }
            }
        }

        Map<String, MobCategory> mobCategoryMap = Map.of(
                "isMonster", MobCategory.MONSTER,
                "isCreature", MobCategory.CREATURE,
                "isMisc", MobCategory.MISC,
                "isWaterCreature", MobCategory.WATER_CREATURE
        );
        for (var entry : mobCategoryMap.entrySet())
        {
            TargetTypes.ENTITY.registerSelectorBuilder(50, (str, fileName) -> {
                if (str.equals(entry.getKey()))
                    return new IsMobCategorySelector(entry.getValue());
                return null;
            });
        }

        TargetTypes.ENTITY.registerSelectorBuilder(50, (str, fileName) -> {
            if (str.equals("isEnemy"))
                return new IsEnemySelector();
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

    /** @return 0 = not a unique op, 1 = {@code make_unique} (per-type), 2 = {@code make_unique_shared} (pooled). */
    private static int uniqueMode(com.google.gson.JsonObject obj)
    {
        var opElement = obj.get("operation");
        if (opElement == null)
            return 0;
        var op = opElement.getAsString();
        if (op.equalsIgnoreCase("make_unique_shared") || op.equalsIgnoreCase("unique_shared"))
            return 2;
        if (op.equalsIgnoreCase("make_unique") || op.equalsIgnoreCase("unique"))
            return 1;
        return 0;
    }

    private static UniqueRegistry.Rule parseUniqueRule(com.google.gson.JsonObject obj, String id, boolean shared)
    {
        var limitElement = obj.get("limit");
        if (limitElement == null)
            limitElement = obj.get("count");
        if (limitElement == null)
            limitElement = obj.get("max");
        if (limitElement == null)
        {
            Attributesetter.LOGGER.error("Missing limit for make_unique entry {}", id);
            return null;
        }
        int limit = Math.max(0, limitElement.getAsInt());
        boolean broadcast = obj.has("broadcast") ? obj.get("broadcast").getAsBoolean() : Config.broadcastByDefault();
        String message = obj.has("message") ? obj.get("message").getAsString() : null;
        return new UniqueRegistry.Rule(limit, shared ? id : null, broadcast, message);
    }

    private static float getMultiplier(com.google.gson.JsonObject obj)
    {
        return obj.has("multiplier") ? obj.get("multiplier").getAsFloat() : 1.0f;
    }

    private static float getOffset(com.google.gson.JsonObject obj)
    {
        return obj.has("offset") ? obj.get("offset").getAsFloat() : 0.0f;
    }

    private void setupSetters()
    {
        // Removal: takes the item / entity out of the game entirely. Registered at the highest priority so it is
        // checked before every other operation. Item removal is accepted in both the `item` folder (itemstack,
        // what people normally use for items) and the `item_type` folder, so either works.
        TargetTypes.ITEMSTACK.registerSetterBuilder(2, (obj, id, selector) -> {
            if (!isRemoveOperation(obj))
                return null;
            return new ItemStackRemoveSetter();
        });
        TargetTypes.ITEM.registerSetterBuilder(2, (obj, id, selector) -> {
            if (!isRemoveOperation(obj))
                return null;
            return new ItemRemoveSetter();
        });
        TargetTypes.ENTITY.registerSetterBuilder(2, (obj, id, selector) -> {
            if (!isRemoveOperation(obj))
                return null;
            return new EntityRemoveSetter();
        });

        // Unique: caps how many copies of an item/entity may ever be created in the save. Same folders as removal.
        // `make_unique` caps each matched type on its own; `make_unique_shared` pools every type the one entry
        // matched into a single counter (so `a || b` limit 1 means only one of the two, ever).
        TargetTypes.ITEMSTACK.registerSetterBuilder(2, (obj, id, selector) -> {
            int mode = uniqueMode(obj);
            if (mode == 0)
                return null;
            var rule = parseUniqueRule(obj, id, mode == 2);
            return rule == null ? null : new ItemStackUniqueSetter(rule);
        });
        TargetTypes.ITEM.registerSetterBuilder(2, (obj, id, selector) -> {
            int mode = uniqueMode(obj);
            if (mode == 0)
                return null;
            var rule = parseUniqueRule(obj, id, mode == 2);
            return rule == null ? null : new ItemUniqueSetter(rule);
        });
        TargetTypes.ENTITY.registerSetterBuilder(2, (obj, id, selector) -> {
            int mode = uniqueMode(obj);
            if (mode == 0)
                return null;
            var rule = parseUniqueRule(obj, id, mode == 2);
            return rule == null ? null : new EntityUniqueSetter(rule);
        });

        // Blocks
        // Removal: turns every generated/placed copy into air and drops the BlockItem.
        TargetTypes.BLOCK.registerSetterBuilder(2, (obj, id, selector) -> {
            if (!isRemoveOperation(obj))
                return null;
            return new BlockRemoveSetter();
        });
        // Replace: swaps the block for another one on generation/placement.
        TargetTypes.BLOCK.registerSetterBuilder(2, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("replace"))
                return null;
            var withElement = obj.get("with");
            if (withElement == null)
                withElement = obj.get("to");
            if (withElement == null)
                withElement = obj.get("block");
            if (withElement == null)
            {
                Attributesetter.LOGGER.error("Missing 'with' block for block replace setter in entry {}", id);
                return null;
            }
            var res = ResourceLocation.parse(withElement.getAsString());
            var block = BuiltInRegistries.BLOCK.getOptional(res).orElse(null);
            if (block == null)
            {
                Attributesetter.LOGGER.error("Failed to find replacement block {} in entry {}", res, id);
                return null;
            }
            return new BlockReplaceSetter(block);
        });
        // Hardness (destroySpeed), blast resistance and mining speed: offset/multiplier tuners.
        TargetTypes.BLOCK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("hardness"))
                return null;
            return new BlockHardnessSetter(getMultiplier(obj), getOffset(obj));
        });
        TargetTypes.BLOCK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("explosion_resistance") && !op.equalsIgnoreCase("blast_resistance"))
                return null;
            return new BlockExplosionResistanceSetter(getMultiplier(obj), getOffset(obj));
        });
        TargetTypes.BLOCK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("mining_speed") && !op.equalsIgnoreCase("break_speed"))
                return null;
            return new BlockMiningSpeedSetter(getMultiplier(obj), getOffset(obj));
        });

        // Simple attribute setters
        TargetTypes.ENTITY.registerSetterBuilder(0, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            var opElement = obj.get("operation");
            if (attrElement != null && valueElement != null)
            {
                var attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrElement.getAsString()));
                var value = valueElement.getAsDouble();
                if (attr == null || attr.isEmpty())
                {
                    Attributesetter.LOGGER.error("Failed to find attribute {}", attrElement.getAsString());
                    return null;
                }
                if (opElement == null || opElement.getAsString().equalsIgnoreCase("base"))
                    return new EntityAttributeSetter(attr.get(), value);
                else
                {
                    AttributeModifier.Operation op = null;
                    switch (opElement.getAsString().toUpperCase())
                    {
                        case "+":
                        case "ADD":
                        case "ADDITION":
                            op = AttributeModifier.Operation.ADD_VALUE;
                            break;
                        case "PERCENT":
                        case "%":
                        case "MULTIPLY_BASE":
                            op = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                            break;
                        case "*":
                        case "x":
                        case "MULTIPLY_TOTAL":
                            op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                            break;
                    }
                    if (op == null)
                    {
                        try
                        {
                            op = AttributeModifier.Operation.valueOf(opElement.getAsString().toUpperCase());
                        } catch (Exception ex)
                        {
                            Attributesetter.LOGGER.error("Failed to parse operation {}", opElement.getAsString());
                            return null;
                        }
                    }
                    return new EntityAttributeModifierSetter(attr.get(), op, value, id);
                }
            }
            return null;
        });

        // Creeper explosion power
        TargetTypes.ENTITY.registerSetterBuilder(1, (obj, id, selector) -> {
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

        // Durability
        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("durability"))
                return null;
            var valueElement = obj.get("value");
            if (valueElement == null)
                return null;
            return new ItemDurabilitySetter(valueElement.getAsInt());
        });

        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
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

        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
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
        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("food"))
                return null;

            if (obj.has("remove") && obj.get("remove").getAsBoolean())
                return FoodNutritionSetter.remove();

            int nutrition;
            float saturation;
            float eatSeconds = 1;
            boolean canAlwaysEat = false;
            ItemStack convertsTo = null;
            List<FoodProperties.PossibleEffect> effects = new ArrayList<>();

            var nutritionElement = obj.get("nutrition");
            var saturationElement = obj.get("saturation");
            var convertsToElement = obj.get("converts_to");
            var effectsElement = obj.get("effects");
            var eatSecondsElement = obj.get("eat_seconds");
            var canAlwaysEatElement = obj.get("can_always_eat");
            if (nutritionElement == null || saturationElement == null)
            {
                Attributesetter.LOGGER.error("Missing nutrition or saturation for food setter in entry {}", id);
                return null;
            }

            nutrition = nutritionElement.getAsInt();
            saturation = saturationElement.getAsFloat();
            if (eatSecondsElement != null)
                eatSeconds = eatSecondsElement.getAsFloat();
            if (canAlwaysEatElement != null)
                canAlwaysEat = canAlwaysEatElement.getAsBoolean();
            if (convertsToElement != null)
            {
                ItemParser parser = new ItemParser(HolderLookup.Provider.create(Stream.of(BuiltInRegistries.REGISTRY.asLookup())));
                ItemResult result = null;
                try {
                    result = parser.parse(new StringReader(convertsToElement.getAsString()));
                } catch (CommandSyntaxException e) {
                    Attributesetter.LOGGER.error("Failed to parse converts_to item for food setter in entry {}", id, e);
                }
                if (result != null)
                    convertsTo = new ItemStack(result.item(), 1, result.components());
                else
                    Attributesetter.LOGGER.error("Failed to parse converts_to item for food setter in entry {}", id);
            }
            if (effectsElement != null && effectsElement.isJsonArray())
            {
                for (var effectElem : effectsElement.getAsJsonArray())
                {
                    if (effectElem.isJsonObject())
                    {
                        var effectObj = effectElem.getAsJsonObject();
                        var effectTypeElem = effectObj.get("effect");
                        var durationElem = effectObj.get("duration");
                        var amplifierElem = effectObj.get("amplifier");
                        var chanceElem = effectObj.get("chance");
                        if (effectTypeElem != null && durationElem != null)
                        {
                            var effectTypeRes = ResourceLocation.parse(effectTypeElem.getAsString());
                            var effectType = BuiltInRegistries.MOB_EFFECT.getHolder(effectTypeRes);
                            if (effectType == null || effectType.isEmpty())
                            {
                                Attributesetter.LOGGER.error("Failed to find mob effect {} for food setter in entry {}", effectTypeRes, id);
                                continue;
                            }
                            int duration = durationElem.getAsInt();
                            int amplifier = amplifierElem != null ? amplifierElem.getAsInt() : 0;
                            float chance = chanceElem != null ? chanceElem.getAsFloat() : 1.0f;
                            var effect = new MobEffectInstance(effectType.get(), duration, amplifier);
                            effects.add(new FoodProperties.PossibleEffect(() -> effect, chance));
                        }
                        else
                            Attributesetter.LOGGER.error("Missing type or duration for food effect in entry {}", id);
                    }
                    else
                        Attributesetter.LOGGER.error("Invalid effect entry in effects array for food setter in entry {}", id);
                }
            }

            return new FoodNutritionSetter(nutrition, saturation, canAlwaysEat, eatSeconds, convertsTo, effects);
        });

        // Food Modify
        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
            var opEl = obj.get("operation");
            if (opEl == null || !opEl.getAsString().equalsIgnoreCase("food_modify"))
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
            float saturationOffset = saturationOffElement != null ? saturationOffElement.getAsInt() : 0;
            int nutritionOffset = nutritionOffElement != null ? nutritionOffElement.getAsInt() : 0;
            float eatSecondsOffset = eatSecondsOffElement != null ? eatSecondsOffElement.getAsFloat() : 0;

            return new FoodNutritionMultiplierSetter(nutritionMultiplier, saturationMultiplier, eatSecondsMultiplier, nutritionOffset, saturationOffset, eatSecondsOffset);
        });


        // Item setters: split by operation
        // - priority 1: base (checked first)
        // - priority 0: modifier (default)
        // Base
        TargetTypes.ITEMSTACK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("base"))
                return null;

            var attrElement = obj.get("attribute");
            var valueElement = obj.get("value");
            if (attrElement == null || valueElement == null)
                return null;

            var attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null || attr.isEmpty())
            {
                Attributesetter.LOGGER.error("Failed to find attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }

            var slot = parseItemSlot(obj.get("slot"), id, selector);
            if (slot == null)
                return null;

            return new ItemAttributeBaseSetter(attr.get(), value, slot, id);
        });

        // Modifiers
        TargetTypes.ITEMSTACK.registerSetterBuilder(0, (obj, id, selector) -> {
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

            var attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrElement.getAsString()));
            var value = valueElement.getAsDouble();
            if (attr == null || attr.isEmpty())
            {
                Attributesetter.LOGGER.error("Failed to find attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }

            var slot = parseItemSlot(obj.get("slot"), id, selector);
            if (slot == null)
                return null;

            if (opElement == null)
                return new ItemAttributeModifierSetter(attr.get(), AttributeModifier.Operation.ADD_VALUE, value, slot, id);

            AttributeModifier.Operation op = null;
            switch (opElement.getAsString().toUpperCase())
            {
                case "+":
                case "ADD":
                case "ADDITION":
                    op = AttributeModifier.Operation.ADD_VALUE;
                    break;
                case "PERCENT":
                case "%":
                case "MULTIPLY_BASE":
                    op = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    break;
                case "*":
                case "x":
                case "MULTIPLY_TOTAL":
                    op = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    break;
            }
            if (op == null)
            {
                try
                {
                    op = AttributeModifier.Operation.valueOf(opElement.getAsString().toUpperCase());
                } catch (Exception ex)
                {
                    Attributesetter.LOGGER.error("Failed to parse operation {} in entry {}", opElement.getAsString(), id);
                    return null;
                }
            }
            return new ItemAttributeModifierSetter(attr.get(), op, value, slot, id);
        });

        // Conversion
        TargetTypes.ITEMSTACK.registerSetterBuilder(1, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var valueElement = obj.get("amount");
            var rateElement = obj.get("rate");
            var opElement = obj.get("operation");
            var fromElement = obj.get("from");
            if (attrElement == null || opElement == null || fromElement == null)
                return null;
            if (!opElement.getAsString().equalsIgnoreCase("conversion"))
                return null;
            var toAttr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrElement.getAsString()));
            var fromAttr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(fromElement.getAsString()));
            var amountConverted = valueElement != null ? valueElement.getAsFloat() : 1.0f;
            var conversionRate = rateElement != null ? rateElement.getAsFloat() : 1.0f;
            if (fromAttr == null || fromAttr.isEmpty())
            {
                Attributesetter.LOGGER.error("Failed to find source attribute {} in entry {}", fromElement.getAsString(), id);
                return null;
            }

            return new ItemAttributeConversionSetter(fromAttr.get(), toAttr.get(), amountConverted, conversionRate, id.toString());
        });
        // Dependency
        TargetTypes.ITEMSTACK.registerSetterBuilder(1, (obj, id, selector) -> {
            var attrElement = obj.get("attribute");
            var multiplierElement = obj.get("multiplier");
            var opElement = obj.get("operation");
            var dependencyElement = obj.get("dependency");
            if (attrElement == null || opElement == null || multiplierElement == null)
                return null;
            if (!opElement.getAsString().equalsIgnoreCase("dependency"))
                return null;
            var attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrElement.getAsString()));
            var dependency = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(dependencyElement.getAsString()));
            var multiplier = multiplierElement != null ? multiplierElement.getAsFloat() : 1.0f;
            if (attr == null || attr.isEmpty())
            {
                Attributesetter.LOGGER.error("Failed to find target attribute {} in entry {}", attrElement.getAsString(), id);
                return null;
            }
            if (dependency == null || dependency.isEmpty())
            {
                Attributesetter.LOGGER.error("Failed to find dependency attribute {} in entry {}", dependencyElement.getAsString(), id);
                return null;
            }

            return new ItemAttributeDependencySetter(attr.get(), dependency.get(), multiplier, id.toString());
        });

        // Tooltip: add simple lines
        TargetTypes.ITEMSTACK.registerSetterBuilder(1, (obj, id, selector) -> {
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

            java.util.List<Component> comps = new java.util.ArrayList<>();
            if (tooltipElement.isJsonArray())
            {
                for (var elem : tooltipElement.getAsJsonArray())
                {
                    try {
                        if (elem.isJsonPrimitive())
                            comps.add(Component.literal(elem.getAsString()));
                        else
                            comps.add(Component.Serializer.fromJson(elem.toString(), ra));
                    } catch (Exception ex) {
                        Attributesetter.LOGGER.error("Failed to parse tooltip component in entry {}", id, ex);
                    }
                }
            }
            else if (tooltipElement.isJsonPrimitive())
            {
                comps.add(Component.literal(tooltipElement.getAsString()));
            }
            if (comps.isEmpty())
                return null;

            return new ItemTooltipAddSetter(comps.toArray(new Component[0]));
        });

        // Tooltip: modify (insert/replace/remove)
        TargetTypes.ITEMSTACK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("tooltip_modify") && !op.equalsIgnoreCase("tooltipmodify") && !op.equalsIgnoreCase("tooltip_modify"))
                return null;

            int index = obj.has("index") ? obj.get("index").getAsInt() : 0;
            var typeElement = obj.get("type");
            ItemTooltipModifySetter.Type type = ItemTooltipModifySetter.Type.INSERT;
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
            List<Component> comps = new ArrayList<>();
            if (componentsElement != null)
            {
                if (componentsElement.isJsonArray())
                {
                    for (var elem : componentsElement.getAsJsonArray())
                    {
                        try {
                            if (elem.isJsonPrimitive())
                                comps.add(Component.literal(elem.getAsString()));
                            else
                                comps.add(Component.Serializer.fromJson(elem.toString(), ra));
                        } catch (Exception ex) {
                            Attributesetter.LOGGER.error("Failed to parse tooltip component in entry {}", id, ex);
                        }
                    }
                }
                else if (componentsElement.isJsonPrimitive())
                {
                    comps.add(Component.literal(componentsElement.getAsString()));
                }
            }

            if (type != ItemTooltipModifySetter.Type.REMOVE && comps.isEmpty())
                return null;

            return new ItemTooltipModifySetter(index, type, comps.toArray(new Component[0]));
        });

        // Attribute Injection
        TargetTypes.ATTRIBUTE.registerSetterBuilder(1, (obj, id, selector) -> {
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

            var source = ResourceLocation.parse(sourceElement.getAsString());
            float multiplier = obj.has("multiplier") ? obj.get("multiplier").getAsFloat() : 1.0f;

            AttributeModifiersInjectionSetter.InjectionType type = AttributeModifiersInjectionSetter.InjectionType.INJECT;
            var typeElement = obj.get("type");
            if (typeElement != null)
            {
                try
                {
                    type = AttributeModifiersInjectionSetter.InjectionType.valueOf(typeElement.getAsString().toUpperCase());
                } catch (Exception ex)
                {
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
                        AttributeModifier.Operation parsedOp = null;
                        switch (opElem.getAsString().toUpperCase())
                        {
                            case "+":
                            case "ADD":
                            case "ADDITION":
                                parsedOp = AttributeModifier.Operation.ADD_VALUE;
                                break;
                            case "PERCENT":
                            case "%":
                            case "MULTIPLY_BASE":
                                parsedOp = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                                break;
                            case "*":
                            case "X":
                            case "MULTIPLY_TOTAL":
                                parsedOp = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                                break;
                        }
                        if (parsedOp == null)
                        {
                            try
                            {
                                parsedOp = AttributeModifier.Operation.valueOf(opElem.getAsString().toUpperCase());
                            } catch (Exception ex)
                            {
                                Attributesetter.LOGGER.error("Failed to parse operation {} in entry {}", opElem.getAsString(), id);
                                return null;
                            }
                        }
                        operations.add(parsedOp);
                    }
                }
                else if (operationsElement.isJsonPrimitive())
                {
                    AttributeModifier.Operation parsedOp = null;
                    switch (operationsElement.getAsString().toUpperCase())
                    {
                        case "+":
                        case "ADD":
                        case "ADDITION":
                            parsedOp = AttributeModifier.Operation.ADD_VALUE;
                            break;
                        case "PERCENT":
                        case "%":
                        case "MULTIPLY_BASE":
                            parsedOp = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                            break;
                        case "*":
                        case "X":
                        case "MULTIPLY_TOTAL":
                            parsedOp = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                            break;
                    }
                    if (parsedOp == null)
                    {
                        try
                        {
                            parsedOp = AttributeModifier.Operation.valueOf(operationsElement.getAsString().toUpperCase());
                        } catch (Exception ex)
                        {
                            Attributesetter.LOGGER.error("Failed to parse operation {} in entry {}", operationsElement.getAsString(), id);
                            return null;
                        }
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

    private EquipmentSlot parseItemSlot(JsonElement slotElement, String id, ASSelector<ItemStack> selector)
    {
        if (slotElement == null)
        {
            IdSelector<?> idSelector;
            if (selector instanceof IdSelector<?> iis)
                idSelector = iis;
            else if (selector instanceof CompositeASSelector<?> cas)
            {
                IdSelector<?> found = null;
                for (var selObj : cas.selectors)
                {
                    if (selObj instanceof IdSelector<?> iis2)
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
                var itemEntry = BuiltInRegistries.ITEM.get(idSelector.id);
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

    // Client-only listeners are registered from the constructor when running on client.
}
