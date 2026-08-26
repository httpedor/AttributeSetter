package com.httpedro.attributesetter.selectors.recipe;

import java.util.regex.Pattern;

import com.httpedro.attributesetter.selectors.ASSelector;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Matches a recipe by its recipe type - {@code minecraft:crafting}, {@code minecraft:smelting}, and so on.
 * Either a single exact type id, or a regex run against the type id string.
 */
public class RecipeTypeSelector extends ASSelector<RecipeHolder<?>> {
    /** The exact type id to match, or {@code null} when matching by regex. */
    public final ResourceLocation type;
    /** The regex to match the type id against, or {@code null} when matching an exact id. */
    public final Pattern pattern;

    private RecipeTypeSelector(ResourceLocation type, Pattern pattern) {
        this.type = type;
        this.pattern = pattern;
    }

    public static RecipeTypeSelector exact(ResourceLocation type) {
        return new RecipeTypeSelector(type, null);
    }

    public static RecipeTypeSelector regex(String regex, boolean ignoreCase) {
        return new RecipeTypeSelector(null, Pattern.compile(regex, ignoreCase ? Pattern.CASE_INSENSITIVE : 0));
    }

    @Override
    protected boolean testImpl(RecipeHolder<?> holder) {
        var id = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        if (id == null)
            return false;
        if (type != null)
            return type.equals(id);
        return pattern.matcher(id.toString()).matches();
    }

    @Override
    public float getSpecificity() {
        // A whole recipe type is broad - broader than an ingredient/result match, narrower than "everything".
        return type != null ? 15 : 12;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
