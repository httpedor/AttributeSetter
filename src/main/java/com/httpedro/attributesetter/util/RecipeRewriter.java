package com.httpedro.attributesetter.util;

import java.util.function.Predicate;

import com.httpedro.attributesetter.Attributesetter;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Rebuilds a recipe with a different result, or with some of its ingredients swapped out. Recipes are immutable
 * and every type has its own constructor, so this reconstructs the vanilla types it recognises field by field.
 *
 * <p>A recipe type it does not know how to rebuild is reported as unsupported (returns {@code null}), and the
 * caller keeps the original untouched rather than dropping it - editing a recipe you can't fully reconstruct is
 * never worth silently corrupting it.
 */
public final class RecipeRewriter {
    private RecipeRewriter() {}

    /** @return a copy of {@code recipe} whose result is {@code newResult}, or {@code null} if unsupported. */
    public static Recipe<?> withResult(Recipe<?> recipe, ItemStack newResult) {
        if (recipe instanceof ShapedRecipe shaped) {
            return new ShapedRecipe(shaped.getGroup(), shaped.category(), shaped.pattern, newResult, shaped.showNotification());
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            return new ShapelessRecipe(shapeless.getGroup(), shapeless.category(), newResult, shapeless.getIngredients());
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            return cooking(cooking, cooking.getIngredients().get(0), newResult);
        }
        if (recipe instanceof StonecutterRecipe stonecutter) {
            return new StonecutterRecipe(stonecutter.getGroup(), stonecutter.getIngredients().get(0), newResult);
        }
        Attributesetter.LOGGER.warn("Can't replace the result of a {} recipe; leaving it alone", recipe.getType());
        return null;
    }

    /**
     * @return a copy of {@code recipe} with every ingredient matching {@code matches} replaced by
     *         {@code replacement}, or {@code null} if the type is unsupported. Returns the same recipe object
     *         when nothing matched, so the caller can tell "no change" apart from "unsupported".
     */
    public static Recipe<?> withReplacedIngredients(Recipe<?> recipe, Predicate<Ingredient> matches, Ingredient replacement) {
        if (recipe instanceof ShapelessRecipe shapeless) {
            var swapped = swap(shapeless.getIngredients(), matches, replacement);
            if (swapped == null)
                return recipe;
            return new ShapelessRecipe(shapeless.getGroup(), shapeless.category(), resultOf(shapeless), swapped);
        }
        if (recipe instanceof ShapedRecipe shaped) {
            var swapped = swap(shaped.pattern.ingredients(), matches, replacement);
            if (swapped == null)
                return recipe;
            var pattern = new ShapedRecipePattern(shaped.pattern.width(), shaped.pattern.height(), swapped, java.util.Optional.empty());
            return new ShapedRecipe(shaped.getGroup(), shaped.category(), pattern, resultOf(shaped), shaped.showNotification());
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            var ingredient = cooking.getIngredients().get(0);
            if (!matches.test(ingredient))
                return recipe;
            return cooking(cooking, replacement, resultOf(cooking));
        }
        if (recipe instanceof StonecutterRecipe stonecutter) {
            var ingredient = stonecutter.getIngredients().get(0);
            if (!matches.test(ingredient))
                return recipe;
            return new StonecutterRecipe(stonecutter.getGroup(), replacement, resultOf(stonecutter));
        }
        Attributesetter.LOGGER.warn("Can't replace ingredients of a {} recipe; leaving it alone", recipe.getType());
        return null;
    }

    /** @return the ingredient list with matches swapped, or {@code null} when nothing matched. */
    private static NonNullList<Ingredient> swap(NonNullList<Ingredient> ingredients, Predicate<Ingredient> matches, Ingredient replacement) {
        boolean changed = false;
        var out = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);
        for (int i = 0; i < ingredients.size(); i++) {
            var ingredient = ingredients.get(i);
            if (!ingredient.isEmpty() && matches.test(ingredient)) {
                out.set(i, replacement);
                changed = true;
            } else {
                out.set(i, ingredient);
            }
        }
        return changed ? out : null;
    }

    /** Rebuilds a cooking recipe of the same kind with a new ingredient and/or result. */
    private static AbstractCookingRecipe cooking(AbstractCookingRecipe cooking, Ingredient ingredient, ItemStack result) {
        var group = cooking.getGroup();
        var category = cooking.category();
        var exp = cooking.getExperience();
        var time = cooking.getCookingTime();
        if (cooking instanceof BlastingRecipe)
            return new BlastingRecipe(group, category, ingredient, result, exp, time);
        if (cooking instanceof SmokingRecipe)
            return new SmokingRecipe(group, category, ingredient, result, exp, time);
        if (cooking instanceof CampfireCookingRecipe)
            return new CampfireCookingRecipe(group, category, ingredient, result, exp, time);
        // Plain furnace smelting, and the fallback for any cooking recipe that isn't one of the special kinds.
        return new SmeltingRecipe(group, category, ingredient, result, exp, time);
    }

    private static ItemStack resultOf(Recipe<?> recipe) {
        return recipe.getResultItem(Attributesetter.ra);
    }
}
