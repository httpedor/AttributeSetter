package com.httpedor.attributesetter.mixin;

import com.httpedor.attributesetter.ASLivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements ASLivingEntity {

    @Unique
    public boolean as$loaded = false;

    @Inject(method = "writeCustomDataToNbt", at = @At(value = "HEAD"))
    public void save(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("ASLoaded", as$loaded);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
    public void load(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("ASLoaded", NbtElement.BYTE_TYPE))
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
