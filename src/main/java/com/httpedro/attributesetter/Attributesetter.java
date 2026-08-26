package com.httpedro.attributesetter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
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
import com.google.gson.JsonObject;
import com.httpedro.attributesetter.api.AttributeSetterAPI;
import com.httpedro.attributesetter.api.TargetType;
import com.httpedro.attributesetter.compat.curios.CuriosCompat;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.AlwaysSelector;
import com.httpedro.attributesetter.selectors.CompositeASSelector;
import com.httpedro.attributesetter.selectors.NamespaceSelector;
import com.httpedro.attributesetter.selectors.IdSelector;
import com.httpedro.attributesetter.selectors.NbtSelector;
import com.httpedro.attributesetter.selectors.RegexSelector;
import com.httpedro.attributesetter.selectors.TagSelector;
import com.httpedro.attributesetter.selectors.HasComponentSelector;
import com.httpedro.attributesetter.selectors.recipe.IngredientSelector;
import com.httpedro.attributesetter.selectors.recipe.RecipeTypeSelector;
import com.httpedro.attributesetter.selectors.recipe.ResultSelector;
import com.httpedro.attributesetter.setters.recipe.RecipeRemoveSetter;
import com.httpedro.attributesetter.setters.recipe.RecipeReplaceIngredientSetter;
import com.httpedro.attributesetter.setters.recipe.RecipeReplaceResultSetter;
import com.httpedro.attributesetter.util.JsonHelper;
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
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
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
        setupSetterShorthands();
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
        // Names for "has this data component". Each is also reachable the long way round, as
        // {"has_component": "minecraft:food"} - these are just the ones common enough to deserve a word.
        Map<String, DataComponentType<?>> componentShorthands = new LinkedHashMap<>();
        componentShorthands.put("isFood", DataComponents.FOOD);
        componentShorthands.put("hasDurability", DataComponents.MAX_DAMAGE);
        componentShorthands.put("isEnchanted", DataComponents.ENCHANTMENTS);
        componentShorthands.put("isPotion", DataComponents.POTION_CONTENTS);
        componentShorthands.put("isTool", DataComponents.TOOL);
        componentShorthands.put("isDyeable", DataComponents.DYED_COLOR);
        componentShorthands.put("isFireResistant", DataComponents.FIRE_RESISTANT);
        componentShorthands.put("hasAttributes", DataComponents.ATTRIBUTE_MODIFIERS);

        for (var targetType : AttributeSetterAPI.getAllTargetTypes())
            setupSelectorsFor(targetType, componentShorthands);

        setupEntitySelectors();
        setupRecipeSelectors();
    }

    /**
     * Registers every selector a target type can support, working out what it can do from the interfaces it
     * implements. Each one goes in twice over: as an object under a type name (which doubles as a field name in
     * a type-less selector object), and as the shorthand string people write in the common case.
     *
     * <p>Nothing here is registered per target type by hand, and a target type that derives from another one
     * inherits whatever the parent got - which is why an item selector like {@code isFood}, or a plain id,
     * works just as well on an item stack.
     */
    @SuppressWarnings("unchecked")
    private <T> void setupSelectorsFor(TargetType<T, ?> targetType, Map<String, DataComponentType<?>> componentShorthands)
    {
        // ---- matches everything -------------------------------------------------------------------------
        targetType.registerJsonSelector((obj, fileName) -> new AlwaysSelector<>(), "always", "everything", "any_target");
        targetType.registerSelectorBuilder(80, (str, fileName) ->
                str.equals("*") || str.equalsIgnoreCase("everything") ? new AlwaysSelector<>() : null);

        // ---- boolean combinators ------------------------------------------------------------------------
        targetType.registerJsonSelector((obj, fileName) -> combine(
                parseSelectorList(targetType, JsonHelper.first(obj, "selectors", "value", "all", "and", "of"), fileName),
                CompositeASSelector.Mode.AND), "all", "and", "all_of");
        targetType.registerJsonSelector((obj, fileName) -> combine(
                parseSelectorList(targetType, JsonHelper.first(obj, "selectors", "value", "any", "or", "of"), fileName),
                CompositeASSelector.Mode.OR), "any", "or", "any_of");
        targetType.registerJsonSelector((obj, fileName) -> {
            var inner = targetType.parseSelector(JsonHelper.first(obj, "selector", "value", "not", "of"), fileName);
            if (inner == null)
                return null;
            inner.inverted = !inner.inverted;
            return inner;
        }, "not", "none_of", "except");

        // `a || b` and `a && b`. Highest priority, so the parts are handed back through the parser one by one.
        targetType.registerSelectorBuilder(Integer.MAX_VALUE, (str, fileName) -> {
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

            List<ASSelector<T>> parts = new ArrayList<>();
            for (var part : str.split(delimiter))
            {
                var parsed = targetType.parseSelector(part.trim(), fileName);
                if (parsed != null)
                    parts.add(parsed);
            }
            return combine(parts, mode);
        });
        // `!something`
        targetType.registerSelectorBuilder(Integer.MAX_VALUE - 1, (str, fileName) -> {
            if (!str.startsWith("!"))
                return null;
            var inner = targetType.parseSelector(str.substring(1).trim(), fileName);
            if (inner == null)
                return null;
            inner.inverted = !inner.inverted;
            return inner;
        });

        // ---- anything with a registry id ----------------------------------------------------------------
        if (targetType instanceof IIdentifiableTargetType)
        {
            var itt = (IIdentifiableTargetType<T>) targetType;

            targetType.registerJsonSelector((obj, fileName) -> {
                List<ASSelector<T>> parts = new ArrayList<>();
                for (var raw : JsonHelper.strings(JsonHelper.first(obj, "id", "ids", "value", "name")))
                {
                    var res = parseId(raw, fileName);
                    if (res == null)
                    {
                        LOGGER.warn("'{}' is not a valid id (file '{}')", raw, fileName);
                        continue;
                    }
                    parts.add(new IdSelector<>(res, itt::getId));
                }
                return combine(parts, CompositeASSelector.Mode.OR);
            }, "id", "ids", "name");

            targetType.registerJsonSelector((obj, fileName) -> {
                var pattern = JsonHelper.string(obj, null, "pattern", "regex", "value", "matches");
                if (pattern == null)
                    return null;
                boolean ignoreCase = JsonHelper.bool(obj, false, "ignore_case", "ignorecase", "case_insensitive");
                var part = switch (JsonHelper.string(obj, "full", "match", "part", "against").toLowerCase(Locale.ROOT)) {
                    case "path", "name" -> RegexSelector.Part.PATH;
                    case "namespace", "mod" -> RegexSelector.Part.NAMESPACE;
                    default -> RegexSelector.Part.FULL;
                };
                return RegexSelector.byId(pattern, itt::getId, part, ignoreCase);
            }, "regex", "pattern", "matches");

            targetType.registerJsonSelector((obj, fileName) -> {
                List<ASSelector<T>> parts = new ArrayList<>();
                for (var ns : JsonHelper.strings(JsonHelper.first(obj, "namespace", "mod", "modid", "value")))
                    parts.add(new NamespaceSelector<>(ns, itt::getId));
                return combine(parts, CompositeASSelector.Mode.OR);
            }, "namespace", "mod", "modid");

            // "regex:.*_sword"
            targetType.registerSelectorBuilder(99, (str, fileName) ->
                    str.startsWith("regex:") ? RegexSelector.byId(str.substring("regex:".length()).trim(), itt::getId) : null);
            // "@somemod" - everything that mod added.
            targetType.registerSelectorBuilder(70, (str, fileName) ->
                    str.startsWith("@") && str.length() > 1 ? new NamespaceSelector<>(str.substring(1).trim(), itt::getId) : null);
            // A bare id is the last thing tried, so a string that is not one can still be claimed by a mod
            // parser of its own rather than being turned into an id that matches nothing.
            targetType.registerSelectorBuilder(Integer.MIN_VALUE, (str, fileName) -> {
                var res = parseId(str, fileName);
                return res == null ? null : new IdSelector<>(res, itt::getId);
            });
        }

        // ---- tags ---------------------------------------------------------------------------------------
        if (targetType instanceof RegistryTargetType)
            setupTagSelectors(targetType, (RegistryTargetType<T, ?>) targetType);
        else if (targetType instanceof IRegistryAssociatedTargetType && targetType instanceof SingletonTargetType)
            registerTagSelectors(targetType, ((IRegistryAssociatedTargetType<T>) targetType).getRegistry(), v -> v);

        // ---- nbt ----------------------------------------------------------------------------------------
        if (targetType instanceof INBTSerializableTargetType)
        {
            var serializer = ((INBTSerializableTargetType<T>) targetType).getSerializer();
            Function<T, CompoundTag> extractor = obj -> {
                var tag = serializer.apply(obj);
                return tag instanceof CompoundTag compound ? compound : new CompoundTag();
            };

            targetType.registerJsonSelector((obj, fileName) -> {
                try {
                    var nbt = JsonHelper.compound(JsonHelper.first(obj, "nbt", "tag", "data", "value"));
                    return nbt == null ? null : new NbtSelector<>(nbt, extractor);
                } catch (Exception e) {
                    LOGGER.error("Could not read the nbt of a selector in file '{}':", fileName, e);
                    return null;
                }
            }, "nbt", "data");

            // "minecraft:diamond_sword{Damage:0}" - an optional selector, then SNBT.
            targetType.registerSelectorBuilder(100, (str, fileName) -> {
                var start = str.indexOf('{');
                var end = str.lastIndexOf('}');
                if (start == -1 || end <= start)
                    return null;
                var before = str.substring(0, start).trim();
                var nbtPart = str.substring(start, end + 1);
                ASSelector<T> beforeSelector = before.isEmpty() ? null : targetType.parseSelector(before, fileName);
                if (beforeSelector == null && !before.isEmpty())
                    return null;
                try {
                    ASSelector<T> nbtSelector = new NbtSelector<>(nbtPart, extractor);
                    return beforeSelector == null
                            ? nbtSelector
                            : combine(List.of(beforeSelector, nbtSelector), CompositeASSelector.Mode.AND);
                } catch (Exception ex) {
                    LOGGER.error("Failed to parse NBT selector part '{}'", nbtPart, ex);
                    return null;
                }
            });
        }

        // ---- data components ----------------------------------------------------------------------------
        if (targetType instanceof IDataComponentHolderTargetType)
        {
            var holder = (IDataComponentHolderTargetType<T>) targetType;

            targetType.registerJsonSelector((obj, fileName) -> {
                List<ASSelector<T>> parts = new ArrayList<>();
                for (var raw : JsonHelper.strings(JsonHelper.first(obj, "has_component", "component", "components", "value")))
                {
                    var type = componentType(raw);
                    if (type == null)
                    {
                        LOGGER.warn("Unknown data component '{}' (file '{}')", raw, fileName);
                        continue;
                    }
                    parts.add(new HasComponentSelector<>(type, holder::getDataComponentMap));
                }
                // Listing several means "has all of them", which is the useful reading.
                return combine(parts, CompositeASSelector.Mode.AND);
            }, "has_component", "component", "components");

            targetType.registerSelectorBuilder(70, (str, fileName) -> {
                if (!str.startsWith("component:"))
                    return null;
                var type = componentType(str.substring("component:".length()).trim());
                return type == null ? null : new HasComponentSelector<>(type, holder::getDataComponentMap);
            });

            for (var entry : componentShorthands.entrySet())
            {
                final String name = entry.getKey();
                final DataComponentType<?> type = entry.getValue();
                targetType.registerSelectorBuilder(60, (str, fileName) ->
                        str.equalsIgnoreCase(name) ? new HasComponentSelector<>(type, holder::getDataComponentMap) : null);
                targetType.registerJsonSelector((obj, fileName) -> {
                    var selector = new HasComponentSelector<T>(type, holder::getDataComponentMap);
                    // {"isFood": false} reads as "and it must not be food".
                    if (!JsonHelper.bool(obj, true, "value"))
                        selector.inverted = true;
                    return selector;
                }, name.toLowerCase(Locale.ROOT));
            }
        }
    }

    /** Captures the registry element type, so the tag selector can be built without raw types. */
    private <T, S> void setupTagSelectors(TargetType<T, ?> targetType, RegistryTargetType<T, S> registryTarget)
    {
        registerTagSelectors(targetType, registryTarget.getRegistry(), registryTarget::getSingleton);
    }

    private <T, S> void registerTagSelectors(TargetType<T, ?> targetType, Registry<S> registry, Function<T, S> singletonGetter)
    {
        targetType.registerJsonSelector((obj, fileName) -> {
            List<ASSelector<T>> parts = new ArrayList<>();
            for (var raw : JsonHelper.strings(JsonHelper.first(obj, "tag", "tags", "value")))
            {
                var res = ResourceLocation.tryParse(raw.startsWith("#") ? raw.substring(1) : raw);
                if (res == null)
                {
                    LOGGER.warn("'{}' is not a valid tag id (file '{}')", raw, fileName);
                    continue;
                }
                parts.add(new TagSelector<>(res, registry, singletonGetter));
            }
            return combine(parts, CompositeASSelector.Mode.OR);
        }, "tag", "tags");

        targetType.registerSelectorBuilder(50, (str, fileName) -> {
            if (!str.startsWith("#"))
                return null;
            var res = ResourceLocation.tryParse(str.substring(1).trim());
            return res == null ? null : new TagSelector<>(res, registry, singletonGetter);
        });
    }

    /** Selectors that only make sense for a living entity. */
    private void setupEntitySelectors()
    {
        var entity = TargetTypes.ENTITY;

        // Every vanilla category gets its own `isX` word, rather than the handful that used to be hardcoded.
        for (var category : MobCategory.values())
        {
            final MobCategory value = category;
            final String shorthand = "is" + camelCase(category.getName());
            entity.registerSelectorBuilder(50, (str, fileName) ->
                    str.equalsIgnoreCase(shorthand) ? new IsMobCategorySelector(value) : null);
        }
        entity.registerJsonSelector((obj, fileName) -> {
            List<ASSelector<LivingEntity>> parts = new ArrayList<>();
            for (var raw : JsonHelper.strings(JsonHelper.first(obj, "mob_category", "category", "value")))
            {
                var category = mobCategory(raw);
                if (category == null)
                {
                    LOGGER.warn("Unknown mob category '{}' (file '{}')", raw, fileName);
                    continue;
                }
                parts.add(new IsMobCategorySelector(category));
            }
            return combine(parts, CompositeASSelector.Mode.OR);
        }, "mob_category", "category");

        entity.registerSelectorBuilder(50, (str, fileName) ->
                str.equalsIgnoreCase("isEnemy") ? new IsEnemySelector() : null);
        entity.registerJsonSelector((obj, fileName) -> {
            var selector = new IsEnemySelector();
            if (!JsonHelper.bool(obj, true, "value"))
                selector.inverted = true;
            return selector;
        }, "is_enemy", "isenemy", "enemy");
    }

    /**
     * Selectors that only make sense for a recipe. The identifiable ones (by recipe id, regex, namespace, and
     * the boolean combinators) come for free from {@link #setupSelectorsFor}; these three add the rest:
     * matching by recipe type, by an ingredient the recipe accepts, and by the recipe's result. The
     * ingredient/result matchers take a full <em>item</em> selector, parsed by the item target, so anything you
     * can write to pick an item (id, {@code #tag}, {@code regex:}, ...) works to pick which recipes to touch.
     */
    private void setupRecipeSelectors()
    {
        var recipe = TargetTypes.RECIPE;

        // recipe_type: an exact type id ("minecraft:smelting"), or a regex against the type id.
        recipe.registerJsonSelector((obj, fileName) -> {
            var pattern = JsonHelper.string(obj, null, "regex", "pattern");
            if (pattern != null)
                return RecipeTypeSelector.regex(pattern, JsonHelper.bool(obj, false, "ignore_case", "ignorecase"));
            var raw = JsonHelper.string(obj, null, "value", "recipe_type", "type_id", "id");
            if (raw == null)
                return null;
            var res = ResourceLocation.tryParse(raw);
            return res == null ? null : RecipeTypeSelector.exact(res);
            // Not aliased to "type": that key is the reserved selector-type discriminator on a selector object.
        }, "recipe_type");
        recipe.registerJsonSelector((obj, fileName) -> {
            var pattern = JsonHelper.string(obj, null, "value", "regex", "pattern");
            return pattern == null ? null : RecipeTypeSelector.regex(pattern, JsonHelper.bool(obj, false, "ignore_case", "ignorecase"));
        }, "recipe_type_regex", "type_regex");
        // "type:minecraft:smelting"
        recipe.registerSelectorBuilder(60, (str, fileName) -> {
            if (!str.startsWith("type:"))
                return null;
            var res = ResourceLocation.tryParse(str.substring("type:".length()).trim());
            return res == null ? null : RecipeTypeSelector.exact(res);
        });

        // contains_ingredient / contains_result: the value is a full item selector.
        recipe.registerJsonSelector((obj, fileName) -> {
            var itemSelector = TargetTypes.ITEM.parseSelector(JsonHelper.first(obj, "value", "contains_ingredient", "ingredient", "input"), fileName);
            return itemSelector == null ? null : new IngredientSelector(itemSelector);
        }, "contains_ingredient", "ingredient", "input");
        recipe.registerJsonSelector((obj, fileName) -> {
            var itemSelector = TargetTypes.ITEM.parseSelector(JsonHelper.first(obj, "value", "contains_result", "result", "output"), fileName);
            return itemSelector == null ? null : new ResultSelector(itemSelector);
        }, "contains_result", "result", "output");
        // "ingredient:#minecraft:planks", "result:minecraft:diamond" - everything after the prefix is an item
        // selector in its own right, so it can itself be a tag, a regex, and so on.
        recipe.registerSelectorBuilder(60, (str, fileName) -> {
            if (!str.startsWith("ingredient:"))
                return null;
            var itemSelector = TargetTypes.ITEM.parseSelector(str.substring("ingredient:".length()).trim(), fileName);
            return itemSelector == null ? null : new IngredientSelector(itemSelector);
        });
        recipe.registerSelectorBuilder(60, (str, fileName) -> {
            if (!str.startsWith("result:"))
                return null;
            var itemSelector = TargetTypes.ITEM.parseSelector(str.substring("result:".length()).trim(), fileName);
            return itemSelector == null ? null : new ResultSelector(itemSelector);
        });
    }

    /** ANDs/ORs a list of selectors, collapsing the one-element case so simple entries stay simple. */
    private static <T> ASSelector<T> combine(List<ASSelector<T>> parts, CompositeASSelector.Mode mode)
    {
        if (parts.isEmpty())
            return null;
        if (parts.size() == 1)
            return parts.get(0);
        return new CompositeASSelector<>(parts, mode);
    }

    private static <T> List<ASSelector<T>> parseSelectorList(TargetType<T, ?> targetType, JsonElement element, String fileName)
    {
        List<ASSelector<T>> parts = new ArrayList<>();
        for (var item : JsonHelper.list(element))
        {
            var parsed = targetType.parseSelector(item, fileName);
            if (parsed != null)
                parts.add(parsed);
        }
        return parts;
    }

    /** Reads an id, defaulting the namespace to the name of the file it was written in. Never throws. */
    private static ResourceLocation parseId(String str, String fallbackNamespace)
    {
        var namespace = fallbackNamespace;
        var path = str;
        int colon = str.indexOf(':');
        if (colon >= 0)
        {
            namespace = str.substring(0, colon);
            path = str.substring(colon + 1);
        }
        return ResourceLocation.tryBuild(namespace, path);
    }

    private static DataComponentType<?> componentType(String raw)
    {
        var res = ResourceLocation.tryParse(raw);
        return res == null ? null : BuiltInRegistries.DATA_COMPONENT_TYPE.get(res);
    }

    private static MobCategory mobCategory(String raw)
    {
        for (var category : MobCategory.values())
        {
            if (category.getName().equalsIgnoreCase(raw) || category.name().equalsIgnoreCase(raw))
                return category;
        }
        return null;
    }

    /** water_creature -> WaterCreature */
    private static String camelCase(String snake)
    {
        var out = new StringBuilder();
        for (var word : snake.split("_"))
        {
            if (word.isEmpty())
                continue;
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
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
        var limitElement = JsonHelper.first(obj, "limit", "count", "max", "value");
        // A make_unique with no limit written down means exactly one, which is what the name promises.
        int limit = limitElement == null ? 1 : Math.max(0, limitElement.getAsInt());
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

        setupRecipeSetters();

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
            var withElement = JsonHelper.first(obj, "with", "to", "block", "value");
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
            var flat = JsonHelper.first(obj, "value", "hardness");
            if (flat != null && !JsonHelper.hasAny(obj, "multiplier", "offset"))
                return BlockHardnessSetter.absolute(flat.getAsFloat());
            return new BlockHardnessSetter(getMultiplier(obj), getOffset(obj));
        });
        TargetTypes.BLOCK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("explosion_resistance") && !op.equalsIgnoreCase("blast_resistance"))
                return null;
            var flat = JsonHelper.first(obj, "value", "resistance");
            if (flat != null && !JsonHelper.hasAny(obj, "multiplier", "offset"))
                return BlockExplosionResistanceSetter.absolute(flat.getAsFloat());
            return new BlockExplosionResistanceSetter(getMultiplier(obj), getOffset(obj));
        });
        TargetTypes.BLOCK.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null)
                return null;
            var op = opElement.getAsString();
            if (!op.equalsIgnoreCase("mining_speed") && !op.equalsIgnoreCase("break_speed"))
                return null;
            var flat = JsonHelper.first(obj, "value", "speed");
            if (flat != null && !JsonHelper.hasAny(obj, "multiplier", "offset"))
                return BlockMiningSpeedSetter.absolute(flat.getAsFloat());
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

            var powerElement = JsonHelper.first(obj, "power", "value");
            if (powerElement != null)
                return new CreeperExplosionPowerSetter(powerElement.getAsInt());

            var multiplierElement = obj.get("multiplier");
            if (multiplierElement != null)
                return new CreeperExplosionPowerSetter(multiplierElement.getAsFloat());

            Attributesetter.LOGGER.error("Missing power or multiplier for creeper explosion power setter in entry {}", id);
            return null;
        });

        // Durability and stack size: a flat value, a multiplier, or an offset on top of either.
        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("durability"))
                return null;
            var valueElement = JsonHelper.first(obj, "value", "durability");
            var multiplierElement = obj.get("multiplier");
            int offset = JsonHelper.integer(obj, 0, "offset");
            if (valueElement != null)
                return new ItemDurabilitySetter(valueElement.getAsInt()).offset(offset);
            if (multiplierElement != null)
                return new ItemDurabilitySetter(multiplierElement.getAsFloat()).offset(offset);
            if (offset != 0)
                return new ItemDurabilitySetter(0).offset(offset);
            Attributesetter.LOGGER.error("Missing value, multiplier or offset for durability setter in entry {}", id);
            return null;
        });

        TargetTypes.ITEM.registerSetterBuilder(1, (obj, id, selector) -> {
            var opElement = obj.get("operation");
            if (opElement == null || !opElement.getAsString().equalsIgnoreCase("max_stack"))
                return null;
            var valueElement = JsonHelper.first(obj, "value", "max_stack", "size");
            var multiplierElement = obj.get("multiplier");
            int offset = JsonHelper.integer(obj, 0, "offset");
            if (valueElement != null)
                return new ItemMaxStackSetter(valueElement.getAsInt()).offset(offset);
            if (multiplierElement != null)
                return new ItemMaxStackSetter(multiplierElement.getAsFloat()).offset(offset);
            if (offset != 0)
                return new ItemMaxStackSetter(0).offset(offset);
            Attributesetter.LOGGER.error("Missing value, multiplier or offset for max stack size setter in entry {}", id);
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

            var sourceElement = JsonHelper.first(obj, "source", "from", "value");
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

    /**
     * Recipe operations: {@code remove} drops the recipe, {@code replace_result} swaps its output for a fixed
     * item, {@code replace_ingredient} swaps every ingredient matching an item selector for a fixed ingredient.
     */
    private void setupRecipeSetters()
    {
        TargetTypes.RECIPE.registerSetterBuilder(2, (obj, id, selector) ->
                isRemoveOperation(obj) ? new RecipeRemoveSetter() : null);

        TargetTypes.RECIPE.registerSetterBuilder(1, (obj, id, selector) -> {
            var op = JsonHelper.string(obj, "", "operation");
            if (!op.equalsIgnoreCase("replace_result") && !op.equalsIgnoreCase("replaceresult") && !op.equalsIgnoreCase("result"))
                return null;
            var spec = JsonHelper.first(obj, "with", "to", "result", "item", "value");
            if (spec == null)
            {
                LOGGER.error("Missing 'with' item for replace_result in entry {}", id);
                return null;
            }
            var stack = parseResultStack(spec, JsonHelper.integer(obj, 1, "count", "amount"), id);
            return stack == null ? null : new RecipeReplaceResultSetter(stack);
        });

        TargetTypes.RECIPE.registerSetterBuilder(1, (obj, id, selector) -> {
            var op = JsonHelper.string(obj, "", "operation");
            if (!op.equalsIgnoreCase("replace_ingredient") && !op.equalsIgnoreCase("replaceingredient"))
                return null;

            // Which ingredients to replace is written exactly like a contains_ingredient selector.
            var matchElement = JsonHelper.first(obj, "match", "from", "ingredient", "target", "replace");
            if (matchElement == null)
            {
                LOGGER.error("Missing 'match' item selector for replace_ingredient in entry {}", id);
                return null;
            }
            var matchSelector = TargetTypes.ITEM.parseSelector(matchElement, id);
            if (matchSelector == null)
            {
                LOGGER.error("Could not parse the 'match' item selector for replace_ingredient in entry {}", id);
                return null;
            }

            var withElement = JsonHelper.first(obj, "with", "to", "replacement");
            if (withElement == null)
            {
                LOGGER.error("Missing 'with' ingredient for replace_ingredient in entry {}", id);
                return null;
            }
            var replacement = parseIngredient(withElement, id);
            return replacement == null ? null : new RecipeReplaceIngredientSetter(matchSelector, replacement);
        });

        // "replace_result minecraft:diamond" - the generic expansion already lands the item in `value`, so the
        // builder above reads it. "replace_ingredient <match> <with>" needs two positional args, so it's its own.
        TargetTypes.RECIPE.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("replace_ingredient", "replaceingredient"))
                return null;
            if (shorthand.arg(0) == null || shorthand.arg(1) == null)
            {
                LOGGER.error("Setter shorthand '{}' needs an ingredient to match and one to replace it with (entry {})", shorthand.raw, id);
                return null;
            }
            var obj = shorthand.base();
            obj.addProperty("operation", "replace_ingredient");
            obj.addProperty("match", shorthand.arg(0));
            obj.addProperty("with", shorthand.arg(1));
            return obj;
        });
    }

    /** Reads a result item from an id string (with optional components) or a {@code {"item": ...}} object. */
    private static ItemStack parseResultStack(JsonElement element, int count, String id)
    {
        String spec;
        if (element.isJsonObject())
            spec = JsonHelper.string(element.getAsJsonObject(), null, "item", "id");
        else if (element.isJsonPrimitive())
            spec = element.getAsString();
        else
            spec = null;
        if (spec == null)
        {
            Attributesetter.LOGGER.error("Could not read a result item from {} in entry {}", element, id);
            return null;
        }
        try {
            ItemParser parser = new ItemParser(HolderLookup.Provider.create(Stream.of(BuiltInRegistries.REGISTRY.asLookup())));
            ItemResult result = parser.parse(new StringReader(spec));
            return new ItemStack(result.item(), Math.max(1, count), result.components());
        } catch (CommandSyntaxException e) {
            Attributesetter.LOGGER.error("Could not parse result item '{}' in entry {}", spec, id, e);
            return null;
        }
    }

    /** Reads an ingredient from {@code "#tag"} / {@code "item"}, or an array of either. */
    private static net.minecraft.world.item.crafting.Ingredient parseIngredient(JsonElement element, String id)
    {
        List<net.minecraft.world.item.crafting.Ingredient> parts = new ArrayList<>();
        for (var raw : JsonHelper.strings(element))
        {
            var ingredient = parseSingleIngredient(raw, id);
            if (ingredient != null)
                parts.add(ingredient);
        }
        if (parts.isEmpty())
        {
            Attributesetter.LOGGER.error("Could not read a replacement ingredient from {} in entry {}", element, id);
            return null;
        }
        if (parts.size() == 1)
            return parts.get(0);
        // A list of ingredients means "any of these", which is one ingredient accepting every listed item.
        return net.minecraft.world.item.crafting.Ingredient.fromValues(
                parts.stream().flatMap(ing -> java.util.Arrays.stream(ing.getValues())));
    }

    private static net.minecraft.world.item.crafting.Ingredient parseSingleIngredient(String raw, String id)
    {
        if (raw.startsWith("#"))
        {
            var res = ResourceLocation.tryParse(raw.substring(1).trim());
            if (res == null)
            {
                Attributesetter.LOGGER.error("Invalid ingredient tag '{}' in entry {}", raw, id);
                return null;
            }
            return net.minecraft.world.item.crafting.Ingredient.of(
                    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, res));
        }
        var res = ResourceLocation.tryParse(raw.trim());
        if (res == null)
        {
            Attributesetter.LOGGER.error("Invalid ingredient item '{}' in entry {}", raw, id);
            return null;
        }
        var item = BuiltInRegistries.ITEM.getOptional(res).orElse(null);
        if (item == null)
        {
            Attributesetter.LOGGER.error("Unknown ingredient item '{}' in entry {}", raw, id);
            return null;
        }
        return net.minecraft.world.item.crafting.Ingredient.of(item);
    }

    /**
     * Setters written as a plain string instead of an object.
     *
     * <p>Most operations need nothing registered here: the generic expansion in {@link TargetType} already
     * turns {@code "remove"} into {@code {"operation": "remove"}}, {@code "durability 500"} into
     * {@code {"operation": "durability", "value": 500}}, {@code "hardness x0.5"} into a multiplier and
     * {@code "max_stack +8"} into an offset, and copies any {@code key=value} token straight through. What is
     * registered here are the operations whose arguments do not line up with that.
     */
    private void setupSetterShorthands()
    {
        // "remove_food" - reads better than `food remove=true`.
        TargetTypes.ITEM.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("remove_food", "food_remove", "nofood"))
                return null;
            var obj = new JsonObject();
            obj.addProperty("operation", "food");
            obj.addProperty("remove", true);
            return obj;
        });

        // "attribute <id> <amount> [operation]", where a leading + or % on the amount picks the operation:
        //   attribute minecraft:generic.max_health 40      -> whatever the target defaults to
        //   attribute minecraft:generic.attack_damage +5   -> add
        //   attribute minecraft:generic.armor %0.25        -> multiply_base
        //   attribute minecraft:generic.armor 0.25 multiply_total slot=chest
        for (var target : List.of(TargetTypes.ENTITY, TargetTypes.ITEMSTACK))
        {
            target.registerSetterShorthand(10, (shorthand, id) -> {
                if (!shorthand.opIs("attribute", "attr"))
                    return null;
                var attribute = shorthand.arg(0);
                if (attribute == null)
                {
                    LOGGER.error("Setter shorthand '{}' needs an attribute (entry {})", shorthand.raw, id);
                    return null;
                }

                var amount = shorthand.arg(1);
                String operation = shorthand.arg(2);
                var obj = shorthand.base();
                // `+5` is read as an offset by the tokenizer, which is exactly the value we want here.
                if (amount == null && shorthand.offset != null)
                {
                    amount = String.valueOf(shorthand.offset);
                    if (operation == null)
                        operation = "add";
                }
                else if (amount != null && amount.startsWith("%"))
                {
                    amount = amount.substring(1);
                    if (operation == null)
                        operation = "multiply_base";
                }
                obj.remove("offset");

                if (amount == null)
                {
                    LOGGER.error("Setter shorthand '{}' needs an amount (entry {})", shorthand.raw, id);
                    return null;
                }
                obj.addProperty("attribute", attribute);
                obj.add("value", JsonHelper.typed(amount));
                if (operation != null)
                    obj.addProperty("operation", operation);
                else
                    // No operation written: let the target type apply its own default (base for entities, a
                    // plain additive modifier for item stacks).
                    obj.remove("operation");
                return obj;
            });
        }

        // "tooltip <the whole rest of the line>"
        TargetTypes.ITEMSTACK.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("tooltip", "tooltip_add", "tooltipadd"))
                return null;
            if (shorthand.rest.isEmpty())
            {
                LOGGER.error("Setter shorthand '{}' needs some text (entry {})", shorthand.raw, id);
                return null;
            }
            var obj = new JsonObject();
            obj.addProperty("operation", "tooltip_add");
            obj.addProperty("tooltip", shorthand.rest);
            return obj;
        });

        // "tooltip_remove [index]"
        TargetTypes.ITEMSTACK.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("tooltip_remove", "remove_tooltip"))
                return null;
            var obj = new JsonObject();
            obj.addProperty("operation", "tooltip_modify");
            obj.addProperty("type", "REMOVE");
            obj.add("index", JsonHelper.typed(shorthand.arg(0) == null ? "0" : shorthand.arg(0)));
            return obj;
        });

        // "dependency <attribute> <the attribute it scales with> x<multiplier>"
        TargetTypes.ITEMSTACK.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("dependency", "depends_on"))
                return null;
            if (shorthand.arg(0) == null || shorthand.arg(1) == null)
            {
                LOGGER.error("Setter shorthand '{}' needs two attributes (entry {})", shorthand.raw, id);
                return null;
            }
            var obj = shorthand.base();
            obj.addProperty("operation", "dependency");
            obj.addProperty("attribute", shorthand.arg(0));
            obj.addProperty("dependency", shorthand.arg(1));
            return obj;
        });

        // "conversion <from> <to> [amount] [rate]"
        TargetTypes.ITEMSTACK.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("conversion", "convert"))
                return null;
            if (shorthand.arg(0) == null || shorthand.arg(1) == null)
            {
                LOGGER.error("Setter shorthand '{}' needs a source and a target attribute (entry {})", shorthand.raw, id);
                return null;
            }
            var obj = shorthand.base();
            obj.addProperty("operation", "conversion");
            obj.addProperty("from", shorthand.arg(0));
            obj.addProperty("attribute", shorthand.arg(1));
            if (shorthand.arg(2) != null)
                obj.add("amount", JsonHelper.typed(shorthand.arg(2)));
            if (shorthand.arg(3) != null)
                obj.add("rate", JsonHelper.typed(shorthand.arg(3)));
            return obj;
        });

        // "inject <source attribute> [x<multiplier>] [type=...]"
        TargetTypes.ATTRIBUTE.registerSetterShorthand(10, (shorthand, id) -> {
            if (!shorthand.opIs("inject", "injection", "attribute_injection"))
                return null;
            if (shorthand.arg(0) == null)
            {
                LOGGER.error("Setter shorthand '{}' needs a source attribute (entry {})", shorthand.raw, id);
                return null;
            }
            var obj = shorthand.base();
            obj.addProperty("operation", "inject");
            obj.addProperty("source", shorthand.arg(0));
            return obj;
        });
    }

    private EquipmentSlot parseItemSlot(JsonElement slotElement, String id, ASSelector<ItemStack> selector)
    {
        if (slotElement == null)
        {
            // No slot written down: if the entry is about one specific item and that item is armour, the slot
            // it goes in is the only sensible reading. find() looks through composites and projections, so it
            // works whatever shape the selector was written in.
            IdSelector<?> idSelector = selector == null ? null : selector.find(IdSelector.class);
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
