package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.api.RemovalRegistry;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops villager and wandering trader offers that buy or sell a removed item. Offers are generated lazily, so this
 * has to happen when they are read rather than at reload time.
 */
@Mixin(AbstractVillager.class)
public class AbstractVillagerMixin {
    @Inject(method = "getOffers", at = @At("RETURN"))
    private void as$dropRemovedOffers(CallbackInfoReturnable<MerchantOffers> cir) {
        if (!RemovalRegistry.hasRemovedItems())
            return;

        var offers = cir.getReturnValue();
        if (offers == null)
            return;

        offers.removeIf(offer -> RemovalRegistry.isRemoved(offer.getResult().getItem())
                || RemovalRegistry.isRemoved(offer.getBaseCostA().getItem())
                || RemovalRegistry.isRemoved(offer.getCostB().getItem()));
    }
}
