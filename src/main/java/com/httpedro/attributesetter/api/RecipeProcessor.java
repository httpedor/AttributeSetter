package com.httpedro.attributesetter.api;

import java.util.ArrayList;
import java.util.List;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.TargetTypes;
import com.httpedro.attributesetter.setters.recipe.RecipeSetter;

import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Runs the recipe target's setters in one pass over the reload's recipe manager. Every recipe is offered to the
 * matching entries in specificity order; each setter takes the recipe the previous one produced, so several
 * edits stack, and a setter returning {@code null} (a {@code remove}) drops the recipe outright.
 *
 * <p>This only runs server-side, during the datapack reload, exactly like the recipe stripping in
 * {@link RemovalRegistry}: the rewritten manager is what vanilla then syncs to every client, so there is no
 * separate client-side recipe handling to keep in step.
 */
public final class RecipeProcessor {
    private RecipeProcessor() {}

    public static void process()
    {
        var recipeManager = RemovalRegistry.getRecipeManager();
        var registries = RemovalRegistry.getRegistries();
        if (recipeManager == null)
            return;
        if (TargetTypes.RECIPE.getAllEntries().isEmpty())
            return;

        var all = recipeManager.getRecipes();
        List<RecipeHolder<?>> result = new ArrayList<>(all.size());
        int removed = 0;
        int rewritten = 0;

        for (var holder : all)
        {
            var setters = TargetTypes.RECIPE.getGenericEntriesFor(holder);
            if (setters.isEmpty())
            {
                result.add(holder);
                continue;
            }

            RecipeHolder<?> current = holder;
            for (var setter : setters)
            {
                if (!(setter instanceof RecipeSetter recipeSetter))
                    continue;
                try {
                    current = recipeSetter.transform(current, registries);
                } catch (Exception e) {
                    Attributesetter.LOGGER.error("Error while applying a recipe setter to {}:", holder.id(), e);
                }
                if (current == null)
                    break; // removed - nothing left to edit
            }

            if (current == null)
                removed++;
            else
            {
                if (current != holder)
                    rewritten++;
                result.add(current);
            }
        }

        if (removed == 0 && rewritten == 0)
            return;

        Attributesetter.LOGGER.info("Recipe pass: removed {} and rewrote {} recipe(s)", removed, rewritten);
        recipeManager.replaceRecipes(result);
    }
}
