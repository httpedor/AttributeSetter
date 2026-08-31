package com.httpedro.attributesetter.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Reads the items an {@link Ingredient} accepts, safely, from inside a datapack reload.
 *
 * <p>{@link Ingredient#getItems()} must never be called during the reload. It resolves tag ingredients through
 * {@code BuiltInRegistries.ITEM.getTagOrEmpty(...)} and then caches the result in the ingredient forever - but the
 * server only binds the reload's tags to the registries <em>after</em> every reload listener has finished
 * (see {@code MinecraftServer.reloadResources} -> {@code updateRegistryTags}). Calling it from our listener would
 * dissolve every tag ingredient we look at against the previous reload's tags, or - on the first load of a world,
 * where nothing is bound yet - against nothing at all, in which case vanilla substitutes a named barrier stack.
 * That cached value is what crafting and the recipe sync then use, so a single lookup silently breaks the recipe
 * for the rest of the session.
 *
 * <p>The reload's real tag contents are available through the condition context NeoForge builds for the reload,
 * which reads the {@code TagManager} result - and the tag manager is the first server reload listener, so its
 * result is already there by the time ours runs.
 */
public final class IngredientItems {
    private IngredientItems() {}

    // Set for the duration of a datapack reload; null outside one, when the registries' own tags are correct.
    private static volatile ICondition.IContext reloadContext = null;

    public static void setReloadContext(ICondition.IContext context)
    {
        reloadContext = context;
    }

    /**
     * Every item this ingredient accepts. Empty for an empty ingredient, and empty (rather than a barrier) for an
     * ingredient whose tag resolved to nothing.
     */
    public static List<Item> itemsOf(Ingredient ingredient)
    {
        if (ingredient == null || ingredient.isEmpty())
            return List.of();

        List<Item> items = new ArrayList<>();

        // Custom ingredients (neoforge:difference, mod-provided types, ...) can only answer for themselves. Ask the
        // custom ingredient directly instead of going through Ingredient#getItems, so that at least the ingredient's
        // own cache is left unpopulated.
        var custom = ingredient.getCustomIngredient();
        if (custom != null)
        {
            custom.getItems().forEach(stack -> {
                if (stack != null && !stack.isEmpty())
                    items.add(stack.getItem());
            });
            return items;
        }

        for (var value : ingredient.getValues())
        {
            if (value instanceof Ingredient.ItemValue itemValue)
            {
                var stack = itemValue.item();
                if (stack != null && !stack.isEmpty())
                    items.add(stack.getItem());
            }
            else if (value instanceof Ingredient.TagValue tagValue)
            {
                var context = reloadContext;
                if (context != null)
                {
                    for (var holder : context.<Item>getTag(tagValue.tag()))
                        items.add(holder.value());
                }
                else
                {
                    for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagValue.tag()))
                        items.add(holder.value());
                }
            }
        }

        return items;
    }
}
