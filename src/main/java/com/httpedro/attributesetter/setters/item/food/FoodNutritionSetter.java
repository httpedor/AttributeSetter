package com.httpedro.attributesetter.setters.item.food;

import java.util.List;
import java.util.Optional;

import com.httpedro.attributesetter.setters.item.ItemComponentsSetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class FoodNutritionSetter extends ItemComponentsSetter {

    public boolean remove;
    public int nutrition;
    public float saturation;
    public float eatSeconds;
    public boolean canAlwaysEat;
    public ItemStack convertsTo;
    public List<FoodProperties.PossibleEffect> effects;

    public FoodNutritionSetter(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds, ItemStack convertsTo, List<FoodProperties.PossibleEffect> effects) {
        remove = false;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.eatSeconds = eatSeconds;
        this.canAlwaysEat = canAlwaysEat;
        this.convertsTo = convertsTo;
        this.effects = effects;
    }
    private FoodNutritionSetter() {
        remove = true;
    }
    public static FoodNutritionSetter remove() {
        return new FoodNutritionSetter();
    }

    @Override
    public void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map) {
        if (remove)
        {
            builder.remove(DataComponents.FOOD);
            return;
        }
        Optional<ItemStack> convertsTo = this.convertsTo == null ? Optional.empty() : Optional.of(this.convertsTo);
        builder.set(DataComponents.FOOD, new FoodProperties(nutrition, saturation, canAlwaysEat, eatSeconds, convertsTo, effects));
    }
}
