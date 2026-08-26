package com.httpedro.attributesetter.selectors.recipe;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.selectors.ASSelector;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Matches a recipe whose result is a matching item, using the same full item selector as
 * {@link IngredientSelector} - {@code {"contains_result": "minecraft:diamond"}},
 * {@code {"contains_result": "#minecraft:planks"}}, and so on.
 */
public class ResultSelector extends ASSelector<RecipeHolder<?>> {
    public final ASSelector<Item> itemSelector;

    public ResultSelector(ASSelector<Item> itemSelector) {
        this.itemSelector = itemSelector;
    }

    @Override
    protected boolean testImpl(RecipeHolder<?> holder) {
        try {
            // Attributesetter.ra is the reload's registry access - set before recipes are processed, and the
            // same handle the removal pass already reads recipe results with.
            var result = holder.value().getResultItem(Attributesetter.ra);
            return result != null && !result.isEmpty() && itemSelector.test(result.getItem());
        } catch (Exception ignored) {
            // A recipe whose result can't be read without crafting it simply doesn't match.
            return false;
        }
    }

    @Override
    public float getSpecificity() {
        return 22;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
