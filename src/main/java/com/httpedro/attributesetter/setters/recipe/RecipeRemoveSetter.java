package com.httpedro.attributesetter.setters.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Drops a matching recipe. Deleting the entry from the datapack brings it back on the next reload. */
public class RecipeRemoveSetter extends RecipeSetter {
    @Override
    public RecipeHolder<?> transform(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        return null;
    }
}
