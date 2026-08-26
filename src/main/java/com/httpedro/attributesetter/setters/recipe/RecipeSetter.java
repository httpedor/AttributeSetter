package com.httpedro.attributesetter.setters.recipe;

import com.httpedro.attributesetter.setters.ASSetter;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * A setter that rewrites a recipe. Recipes are immutable and are never touched in place, so - like the event
 * setters - the plain {@code apply(target)} is not used; the recipe pass calls {@link #transform} to get the
 * replacement recipe (or {@code null} to drop it).
 */
public abstract class RecipeSetter extends ASSetter<RecipeHolder<?>> {
    @Override
    public void apply(RecipeHolder<?> target) {
        throw new UnsupportedOperationException("Recipe setters are applied through the recipe pass, not directly");
    }

    /**
     * @param holder the recipe as it currently stands (already carrying any earlier setters' edits)
     * @param registries the reload's registry access
     * @return the recipe to keep - the same holder if untouched, a rewritten one, or {@code null} to remove it
     */
    public abstract RecipeHolder<?> transform(RecipeHolder<?> holder, HolderLookup.Provider registries);
}
