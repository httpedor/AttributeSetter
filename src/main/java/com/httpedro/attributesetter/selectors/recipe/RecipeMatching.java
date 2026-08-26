package com.httpedro.attributesetter.selectors.recipe;

import com.httpedro.attributesetter.selectors.ASSelector;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * The one place that decides what it means for an item selector to "match" an ingredient. An ingredient can
 * stand for several items (a tag, a list), so it counts as a match when any item it accepts matches the
 * selector. Shared between the {@code contains_ingredient} selector and the {@code replace_ingredient} setter,
 * so "which ingredients does this touch" always means the same thing.
 */
public final class RecipeMatching {
    private RecipeMatching() {}

    public static boolean ingredientMatches(Ingredient ingredient, ASSelector<Item> selector) {
        if (ingredient == null || ingredient.isEmpty())
            return false;
        for (var stack : ingredient.getItems()) {
            if (stack != null && !stack.isEmpty() && selector.test(stack.getItem()))
                return true;
        }
        return false;
    }
}
