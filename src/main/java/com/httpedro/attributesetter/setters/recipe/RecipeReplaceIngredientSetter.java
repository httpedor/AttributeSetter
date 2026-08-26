package com.httpedro.attributesetter.setters.recipe;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.selectors.ASSelector;
import com.httpedro.attributesetter.selectors.recipe.RecipeMatching;
import com.httpedro.attributesetter.util.RecipeRewriter;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Replaces every ingredient a recipe accepts that matches an item selector with a fixed ingredient. "Which
 * ingredients" is decided the exact same way {@code contains_ingredient} decides what it matches, so the two
 * read consistently.
 */
public class RecipeReplaceIngredientSetter extends RecipeSetter {
    private final ASSelector<Item> matches;
    private final Ingredient replacement;

    public RecipeReplaceIngredientSetter(ASSelector<Item> matches, Ingredient replacement) {
        this.matches = matches;
        this.replacement = replacement;
    }

    @Override
    public RecipeHolder<?> transform(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        var rewritten = RecipeRewriter.withReplacedIngredients(
                holder.value(),
                ingredient -> RecipeMatching.ingredientMatches(ingredient, matches),
                replacement);
        if (rewritten == null || rewritten == holder.value())
            return holder; // unsupported type, or nothing in this recipe matched
        Attributesetter.LOGGER.debug("Replaced ingredients of recipe {}", holder.id());
        return new RecipeHolder<>(holder.id(), rewritten);
    }
}
