package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ASLivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements ASLivingEntity {

    @Unique
    public boolean as$loaded = false;

    @Inject(method = "addAdditionalSaveData", at = @At(value = "HEAD"))
    public void save(CompoundTag nbt, CallbackInfo ci) {
        nbt.putBoolean("ASLoaded", as$loaded);
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "HEAD"))
    public void load(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("ASLoaded"))
            as$setLoaded();
    }


    @Override
    public boolean as$isLoaded() {
        return as$loaded;
    }

    @Override
    public void as$setLoaded() {
        as$loaded = true;
    }
}
