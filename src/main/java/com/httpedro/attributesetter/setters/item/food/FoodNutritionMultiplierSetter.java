package com.httpedro.attributesetter.setters.item.food;

import com.httpedro.attributesetter.setters.item.ItemComponentsSetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;

public class FoodNutritionMultiplierSetter extends ItemComponentsSetter {

    public float nutrition;
    public float saturation;
    public float eatSeconds;
    public int nutritionOffset;
    public float saturationOffset;
    public float eatSecondsOffset;

    public FoodNutritionMultiplierSetter(float nutrition, float saturation, float eatSeconds, int nutritionOffset, float saturationOffset, float eatSecondsOffset) {
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.eatSeconds = eatSeconds;
        this.nutritionOffset = nutritionOffset;
        this.saturationOffset = saturationOffset;
        this.eatSecondsOffset = eatSecondsOffset;
    }

    @Override
    public void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map) {
        if (!map.has(DataComponents.FOOD))
            return;

        FoodProperties food = map.get(DataComponents.FOOD);
        builder.set(DataComponents.FOOD, new FoodProperties(Math.round(nutrition * food.nutrition()) + nutritionOffset, saturation * food.saturation() + saturationOffset, food.canAlwaysEat(), eatSeconds * food.eatSeconds() + eatSecondsOffset, food.usingConvertsTo(), food.effects()));
    }
}
