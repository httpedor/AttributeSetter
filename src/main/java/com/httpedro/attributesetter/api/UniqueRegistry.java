package com.httpedro.attributesetter.api;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.Config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds the {@code make_unique} / {@code make_unique_shared} caps and gates every creation source against the
 * per-save counts in {@link UniqueCounts}. This is the unique-item counterpart of {@link RemovalRegistry}: the caps
 * are rebuilt from scratch on every datapack reload, while the counts live with the world.
 *
 * <p>Two shapes of rule:
 * <ul>
 *   <li><b>per-type</b> ({@code make_unique}) - each matched item/entity type gets its own counter, keyed by its
 *       registry id.</li>
 *   <li><b>shared</b> ({@code make_unique_shared}) - every type matched by the one datapack entry shares a single
 *       counter, keyed by that entry's id (the group id).</li>
 * </ul>
 *
 * <p>All the count mutation goes through the single {@link UniqueCounts} instance on the overworld, on the server
 * thread; the maps themselves are only written during reload, so the reads from the loot/craft/spawn hooks don't
 * need locking beyond what the server thread already gives us.
 */
public class UniqueRegistry {
    /** A single cap. {@code groupId == null} means per-type (counter keyed by the type's own id). */
    public record Rule(int limit, String groupId, boolean broadcast, String message) {}

    /** Bookkeeping for a shared group, for command tab-completion and broadcast naming. */
    public static class GroupInfo {
        public final int limit;
        public final boolean broadcast;
        public final String message;
        public final Set<Item> items = new HashSet<>();
        public final Set<EntityType<?>> entities = new HashSet<>();

        GroupInfo(Rule rule)
        {
            this.limit = rule.limit();
            this.broadcast = rule.broadcast();
            this.message = rule.message();
        }
    }

    private static final Map<Item, Rule> itemRules = new HashMap<>();
    private static final Map<EntityType<?>, Rule> entityRules = new HashMap<>();
    private static final Map<String, GroupInfo> groups = new HashMap<>();

    private static MinecraftServer server = null;

    public static void setServer(MinecraftServer s)
    {
        server = s;
    }

    /** Wipes the caps (not the counts) at the start of a reload. */
    public static void clearRules()
    {
        itemRules.clear();
        entityRules.clear();
        groups.clear();
    }

    // ------------------------------------------------------------------------------------------------------ caps

    public static void registerItem(Item item, Rule rule)
    {
        if (item == null || rule == null)
            return;
        itemRules.put(item, rule);
        if (rule.groupId() != null)
            groups.computeIfAbsent(rule.groupId(), k -> new GroupInfo(rule)).items.add(item);
    }

    public static void registerEntity(EntityType<?> type, Rule rule)
    {
        if (type == null || rule == null)
            return;
        entityRules.put(type, rule);
        if (rule.groupId() != null)
            groups.computeIfAbsent(rule.groupId(), k -> new GroupInfo(rule)).entities.add(type);
    }

    public static boolean hasItemRules()
    {
        return !itemRules.isEmpty();
    }

    public static boolean hasEntityRules()
    {
        return !entityRules.isEmpty();
    }

    public static Rule getItemRule(Item item)
    {
        return itemRules.get(item);
    }

    public static Rule getEntityRule(EntityType<?> type)
    {
        return entityRules.get(type);
    }

    public static Set<String> getGroupIds()
    {
        return groups.keySet();
    }

    // --------------------------------------------------------------------------------------------------- counting

    public static String itemKey(Item item)
    {
        return "item:" + BuiltInRegistries.ITEM.getKey(item);
    }

    public static String entityKey(EntityType<?> type)
    {
        return "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public static String groupKey(String groupId)
    {
        return "group:" + groupId;
    }

    private static String keyFor(Rule rule, String perTypeKey)
    {
        return rule.groupId() != null ? groupKey(rule.groupId()) : perTypeKey;
    }

    public static int getCount(String key)
    {
        if (server == null)
            return 0;
        return UniqueCounts.get(server).getCount(key);
    }

    public static void setCount(String key, int value)
    {
        if (server == null)
            return;
        UniqueCounts.get(server).setCount(key, value);
    }

    public static void addCount(String key, int delta)
    {
        setCount(key, getCount(key) + delta);
    }

    /**
     * @return the effective limit for a count key (from the cap that owns it), or -1 when nothing caps it. Used by
     *         the commands to show the limit alongside a count.
     */
    public static int limitForKey(String key)
    {
        if (key.startsWith("group:"))
        {
            var info = groups.get(key.substring("group:".length()));
            return info != null ? info.limit : -1;
        }
        if (key.startsWith("item:"))
        {
            var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(key.substring("item:".length()))).orElse(null);
            var rule = item != null ? itemRules.get(item) : null;
            return rule != null && rule.groupId() == null ? rule.limit() : -1;
        }
        if (key.startsWith("entity:"))
        {
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(key.substring("entity:".length()))).orElse(null);
            var rule = type != null ? entityRules.get(type) : null;
            return rule != null && rule.groupId() == null ? rule.limit() : -1;
        }
        return -1;
    }

    /**
     * How many of {@code amount} copies of {@code item} may be created before hitting the cap. Increments the
     * counter by that many and broadcasts if the cap is reached on this call. Items with no rule return
     * {@code amount} unchanged and touch nothing.
     */
    public static int tryConsumeItem(Item item, int amount, Level level)
    {
        Rule rule = itemRules.get(item);
        if (rule == null || amount <= 0)
            return amount;

        String key = keyFor(rule, itemKey(item));
        int current = getCount(key);
        int remaining = rule.limit() - current;
        if (remaining <= 0)
            return 0;

        int allowed = Math.min(amount, remaining);
        int next = current + allowed;
        setCount(key, next);
        if (current < rule.limit() && next >= rule.limit())
            broadcast(rule, item.getDescription(), level);
        return allowed;
    }

    public static boolean isEntityBlocked(EntityType<?> type)
    {
        Rule rule = entityRules.get(type);
        if (rule == null)
            return false;
        String key = keyFor(rule, entityKey(type));
        return getCount(key) >= rule.limit();
    }

    /**
     * Records one more of {@code type} against its cap and broadcasts if the cap is reached. Callers must have
     * already confirmed the entity is allowed (via {@link #isEntityBlocked}) and that it hasn't been counted before.
     */
    public static void consumeEntity(EntityType<?> type, Level level)
    {
        Rule rule = entityRules.get(type);
        if (rule == null)
            return;
        String key = keyFor(rule, entityKey(type));
        int current = getCount(key);
        int next = current + 1;
        setCount(key, next);
        if (current < rule.limit() && next >= rule.limit())
            broadcast(rule, type.getDescription(), level);
    }

    // -------------------------------------------------------------------------------------------------- broadcast

    private static void broadcast(Rule rule, Component name, Level level)
    {
        if (!rule.broadcast())
            return;
        if (level == null || level.getServer() == null)
            return;

        String template = rule.message();
        if (template == null || template.isBlank())
            template = Config.defaultBroadcastMessage();

        String rendered = template
                .replace("%name%", name.getString())
                .replace("%limit%", Integer.toString(rule.limit()))
                .replace("%count%", Integer.toString(rule.limit()));
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(rendered), false);
        Attributesetter.LOGGER.info("Unique cap reached for {} (limit {})", name.getString(), rule.limit());
    }
}
