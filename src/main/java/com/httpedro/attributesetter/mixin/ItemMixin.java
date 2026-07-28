package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ducktypes.ItemDuckType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin implements ItemDuckType {
    @Mutable
    @Shadow
    @Final
    private int maxStackSize;

    @Mutable
    @Shadow
    @Final
    private FoodProperties foodProperties;

    @Unique
    private Integer attributesetter$eatTicks = null;
    @Unique
    private ItemStack attributesetter$convertsTo = null;

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void as$customEatTicks(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (attributesetter$eatTicks != null)
            cir.setReturnValue(attributesetter$eatTicks);
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"), cancellable = true)
    private void as$convertOnEat(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (attributesetter$convertsTo == null)
            return;
        // Mirrors HoneyBottleItem: give the conversion result once the food is eaten.
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack result = cir.getReturnValue();
            ItemStack conversion = attributesetter$convertsTo.copy();
            if (result.isEmpty()) {
                cir.setReturnValue(conversion);
                return;
            }
            if (!player.getInventory().add(conversion))
                player.drop(conversion, false);
        }
    }

    @Override
    public void as$setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    @Override
    public int as$getMaxStackSize() {
        return this.maxStackSize;
    }

    @Override
    public void as$setFoodProperties(FoodProperties food) {
        this.foodProperties = food;
    }

    @Override
    public FoodProperties as$getFoodProperties() {
        return this.foodProperties;
    }

    @Override
    public void as$setEatTicks(Integer ticks) {
        this.attributesetter$eatTicks = ticks;
    }

    @Override
    public Integer as$getEatTicks() {
        return this.attributesetter$eatTicks;
    }

    @Override
    public void as$setConvertsTo(ItemStack stack) {
        this.attributesetter$convertsTo = stack;
    }

    @Override
    public ItemStack as$getConvertsTo() {
        return this.attributesetter$convertsTo;
    }
}
