package com.httpedro.attributesetter.api;

import com.httpedro.attributesetter.Attributesetter;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds everything a {@code remove} setter deleted from the game. The lists are rebuilt from scratch on every
 * datapack reload (and on every client datapack sync), so deleting the entry from the datapack brings the item or
 * entity back without any restore bookkeeping - unlike the item-global setters, nothing here mutates the item.
 *
 * <p>The sets are read from the render thread (creative tabs), the server thread (loot, spawns, inventories) and
 * during reload, hence the concurrent sets.
 */
public class RemovalRegistry {
    private static final Set<Item> removedItems = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<EntityType<?>> removedEntityTypes = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** Bumped whenever the removal lists change, so caches built from them can tell they went stale. */
    private static volatile int generation = 0;

    // Captured from AddReloadListenerEvent every reload; null on the client (which gets already-filtered recipes).
    private static RecipeManager recipeManager = null;
    private static RegistryAccess registryAccess = null;

    public static void setServerContext(RecipeManager manager, RegistryAccess access)
    {
        recipeManager = manager;
        registryAccess = access;
    }

    public static void clear()
    {
        removedItems.clear();
        removedEntityTypes.clear();
        generation++;
    }

    public static void removeItem(Item item)
    {
        if (item == null || item == Items.AIR)
            return;
        removedItems.add(item);
    }

    /** Removes an entity type, along with its spawn egg (vanilla or modded). */
    public static void removeEntityType(EntityType<?> type)
    {
        if (type == null || type == EntityType.PLAYER)
            return;
        if (!removedEntityTypes.add(type))
            return;

        var egg = ForgeSpawnEggItem.fromEntityType(type);
        if (egg != null)
            removeItem(egg);
    }

    public static boolean isRemoved(Item item)
    {
        return !removedItems.isEmpty() && removedItems.contains(item);
    }

    public static boolean isRemoved(EntityType<?> type)
    {
        return !removedEntityTypes.isEmpty() && removedEntityTypes.contains(type);
    }

    public static boolean hasRemovedItems()
    {
        return !removedItems.isEmpty();
    }

    public static int getGeneration()
    {
        return generation;
    }

    /**
     * Called once every removal entry of a reload has been collected: this is where the one-shot consequences
     * (recipe stripping) happen, since they need the full list.
     */
    public static void finishReload(boolean serverSide)
    {
        generation++;
        if (serverSide)
            filterRecipes();

        if (!removedItems.isEmpty() || !removedEntityTypes.isEmpty())
            Attributesetter.LOGGER.info("Removed {} item(s) and {} entity type(s) from the game", removedItems.size(), removedEntityTypes.size());
    }

    /**
     * Drops every recipe that produces a removed item, or that can no longer be crafted because one of its
     * ingredients only accepted removed items. The recipe manager is reloaded from the datapacks before this runs,
     * so the recipes come back on their own once the removal entry is gone.
     */
    private static void filterRecipes()
    {
        if (recipeManager == null || removedItems.isEmpty())
            return;

        var all = recipeManager.getRecipes();
        List<Recipe<?>> kept = new ArrayList<>(all.size());
        for (var recipe : all)
        {
            if (!isDead(recipe))
                kept.add(recipe);
        }

        if (kept.size() == all.size())
            return;

        Attributesetter.LOGGER.info("Removing {} recipe(s) that involve removed items", all.size() - kept.size());
        recipeManager.replaceRecipes(kept);
    }

    private static boolean isDead(Recipe<?> recipe)
    {
        // Modded recipes can throw here (result assembled from a container, no registry access, ...) - a recipe we
        // can't inspect is a recipe we keep.
        try {
            var result = recipe.getResultItem(registryAccess);
            if (result != null && !result.isEmpty() && removedItems.contains(result.getItem()))
                return true;
        } catch (Exception ex) {
            Attributesetter.LOGGER.debug("Could not read the result of recipe {}", recipe.getId(), ex);
        }

        try {
            for (var ingredient : recipe.getIngredients())
            {
                if (ingredient.isEmpty())
                    continue;
                var stacks = ingredient.getItems();
                if (stacks.length == 0)
                    continue;

                // Only kill the recipe when every option for this ingredient is gone; a tag ingredient that lost
                // one of its items is still craftable with the others.
                boolean allRemoved = true;
                for (var stack : stacks)
                {
                    if (!removedItems.contains(stack.getItem()))
                    {
                        allRemoved = false;
                        break;
                    }
                }
                if (allRemoved)
                    return true;
            }
        } catch (Exception ex) {
            Attributesetter.LOGGER.debug("Could not read the ingredients of recipe {}", recipe.getId(), ex);
        }

        return false;
    }
}
