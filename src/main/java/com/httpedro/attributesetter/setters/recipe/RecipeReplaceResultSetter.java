package com.httpedro.attributesetter.setters.recipe;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.util.RecipeRewriter;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Replaces a recipe's result with a fixed item stack. Unsupported recipe types are left untouched. */
public class RecipeReplaceResultSetter extends RecipeSetter {
    private final ItemStack result;

    public RecipeReplaceResultSetter(ItemStack result) {
        this.result = result;
    }

    @Override
    public RecipeHolder<?> transform(RecipeHolder<?> holder, HolderLookup.Provider registries) {
        var rewritten = RecipeRewriter.withResult(holder.value(), result.copy());
        if (rewritten == null)
            return holder;
        Attributesetter.LOGGER.debug("Replaced the result of recipe {} with {}", holder.id(), result);
        return new RecipeHolder<>(holder.id(), rewritten);
    }
}
