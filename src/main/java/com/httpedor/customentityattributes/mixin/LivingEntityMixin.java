package com.httpedor.customentityattributes.mixin;

import com.httpedor.customentityattributes.ICEALivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements ICEALivingEntity {

    @Unique
    public boolean cea$loaded = false;

    @Inject(method = "writeCustomDataToNbt", at = @At(value = "HEAD"))
    public void save(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("CEALoaded", cea$loaded);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At(value = "HEAD"))
    public void load(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("CEALoaded", NbtElement.BYTE_TYPE))
            cea$setLoaded();
    }

    @Override
    public boolean cea$isLoaded() {
        return cea$loaded;
    }

    @Override
    public void cea$setLoaded() {
        cea$loaded = true;
    }
}
