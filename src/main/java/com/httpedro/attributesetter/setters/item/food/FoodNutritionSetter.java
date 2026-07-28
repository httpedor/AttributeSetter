package com.httpedro.attributesetter.setters.item.food;

import java.util.List;

import com.httpedro.attributesetter.api.TrueDefaults;
import com.httpedro.attributesetter.ducktypes.ItemDuckType;
import com.httpedro.attributesetter.setters.item.ItemSetter;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public class FoodNutritionSetter extends ItemSetter {

    public boolean remove;
    public int nutrition;
    public float saturation;
    public boolean canAlwaysEat;
    public boolean fastFood;
    public Integer eatTicks;
    public ItemStack convertsTo;
    public List<Pair<MobEffectInstance, Float>> effects;

    public FoodNutritionSetter(int nutrition, float saturation, boolean canAlwaysEat, boolean fastFood, Integer eatTicks, ItemStack convertsTo, List<Pair<MobEffectInstance, Float>> effects) {
        this.remove = false;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.canAlwaysEat = canAlwaysEat;
        this.fastFood = fastFood;
        this.eatTicks = eatTicks;
        this.convertsTo = convertsTo;
        this.effects = effects;
    }
    private FoodNutritionSetter() {
        this.remove = true;
    }
    public static FoodNutritionSetter remove() {
        return new FoodNutritionSetter();
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void apply(ItemStack target) {
        var item = target.getItem();
        TrueDefaults.snapshot(item);
        var duck = (ItemDuckType) item;

        if (remove) {
            duck.as$setFoodProperties(null);
            duck.as$setEatTicks(null);
            duck.as$setConvertsTo(null);
            return;
        }

        var builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturation);
        if (canAlwaysEat)
            builder.alwaysEat();
        if (fastFood)
            builder.fast();
        if (effects != null) {
            for (var effect : effects) {
                if (effect != null && effect.getFirst() != null)
                    builder.effect(effect.getFirst(), effect.getSecond());
            }
        }
        duck.as$setFoodProperties(builder.build());
        duck.as$setEatTicks(eatTicks);
        duck.as$setConvertsTo(convertsTo);
    }
}
