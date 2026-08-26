package com.httpedro.attributesetter.selectors.recipe;

import com.httpedro.attributesetter.selectors.ASSelector;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Matches a recipe that accepts a matching item somewhere in its ingredients. What counts as "a matching item"
 * is a full item selector (id, tag, regex, ...), so {@code {"contains_ingredient": "#minecraft:planks"}} and
 * {@code {"contains_ingredient": {"regex": ".*_log"}}} both work.
 */
public class IngredientSelector extends ASSelector<RecipeHolder<?>> {
    public final ASSelector<Item> itemSelector;

    public IngredientSelector(ASSelector<Item> itemSelector) {
        this.itemSelector = itemSelector;
    }

    @Override
    protected boolean testImpl(RecipeHolder<?> holder) {
        try {
            for (var ingredient : holder.value().getIngredients()) {
                if (RecipeMatching.ingredientMatches(ingredient, itemSelector))
                    return true;
            }
        } catch (Exception ignored) {
            // A recipe whose ingredients can't be read (a custom recipe type) simply doesn't match.
        }
        return false;
    }

    @Override
    public float getSpecificity() {
        return 20;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
