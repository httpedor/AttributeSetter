package com.httpedro.attributesetter.setters.item.food;

import com.httpedro.attributesetter.api.TrueDefaults;
import com.httpedro.attributesetter.ducktypes.ItemDuckType;
import com.httpedro.attributesetter.setters.item.ItemSetter;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class FoodNutritionMultiplierSetter extends ItemSetter {

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
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void apply(ItemStack target) {
        var item = target.getItem();
        var duck = (ItemDuckType) item;
        FoodProperties food = duck.as$getFoodProperties();
        if (food == null)
            return;

        TrueDefaults.snapshot(item);

        var builder = new FoodProperties.Builder()
                .nutrition(Math.round(nutrition * food.getNutrition()) + nutritionOffset)
                .saturationMod(saturation * food.getSaturationModifier() + saturationOffset);
        if (food.isMeat())
            builder.meat();
        if (food.canAlwaysEat())
            builder.alwaysEat();
        if (food.isFastFood())
            builder.fast();
        for (var effect : food.getEffects())
            builder.effect(effect.getFirst(), effect.getSecond());
        duck.as$setFoodProperties(builder.build());

        int baseTicks = duck.as$getEatTicks() != null ? duck.as$getEatTicks() : target.getUseDuration();
        duck.as$setEatTicks(Math.round(baseTicks * eatSeconds + eatSecondsOffset * 20));
    }
}
