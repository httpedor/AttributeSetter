package com.httpedro.attributesetter.targettypes;

import java.util.List;

import com.httpedro.attributesetter.api.TargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IIdentifiableTargetType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Recipes, keyed by their datapack id. A recipe has an id but no registry, no tags, no NBT and no data
 * components, so of the shared selectors it only picks up the identifiable ones (id, regex, namespace, and the
 * boolean combinators) - which is exactly right. Everything recipe-specific (matching by recipe type, by an
 * ingredient, or by the result) is registered on top in {@code Attributesetter.setupRecipeSelectors}.
 *
 * <p>Unlike the other target types, recipes are not applied by iterating a registry: they are rewritten in one
 * pass over the server's recipe manager during the datapack reload (see {@code RecipeProcessor}), the same
 * moment - and by the same mechanism - as the recipe stripping the {@code remove} setters already did.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RecipeTargetType extends TargetType<RecipeHolder<?>, ResourceLocation>
        implements IIdentifiableTargetType<RecipeHolder<?>> {

    public RecipeTargetType() {
        super((Class) RecipeHolder.class);
    }

    @Override
    public String getFolderName() {
        return "recipe";
    }

    @Override
    public List<String> getFolderAliases() {
        return List.of("recipes");
    }

    @Override
    public ResourceLocation getId(RecipeHolder<?> obj) {
        return obj.id();
    }

    @Override
    public ResourceLocation getCacheKey(RecipeHolder<?> object) {
        // Each recipe id is unique and each recipe is only ever looked at once per reload, so this key never
        // collides; it exists only because the base class demands one.
        return object.id();
    }
}
