package com.httpedro.attributesetter.mixin;

import net.neoforged.neoforge.internal.RegistrationEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RegistrationEvents.class)
public class RegistrationEventsMixin {

    @Shadow
    private static boolean canModifyComponents;

    @Inject(method = "modifyComponents", at = @At("RETURN"))
    private static void allowComponentModification(CallbackInfo ci) {
        canModifyComponents = true;
    }

}
