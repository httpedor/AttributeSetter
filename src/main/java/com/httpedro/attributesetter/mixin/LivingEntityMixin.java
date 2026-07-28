package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ASLivingEntity;
import com.httpedro.attributesetter.TemporaryAttributeInjection;
import com.httpedro.attributesetter.ducktypes.AttributeSupplierDuckType;
import com.httpedro.attributesetter.ducktypes.EntityAttributeInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements ASLivingEntity {
    @Unique
    protected Map<AttributeModifier.Operation, Map<java.util.UUID, TemporaryAttributeInjection>> as$temporaryInjections = new HashMap<>();

    @Unique
    public boolean as$loaded = false;

    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    @Nullable
    public abstract AttributeInstance getAttribute(Attribute attribute);

    @Override
    public Collection<TemporaryAttributeInjection> as$getInjections()
    {
        return as$temporaryInjections.values().stream().flatMap(m -> m.values().stream()).toList();
    }

    @Override
    public Collection<TemporaryAttributeInjection> as$getInjections(AttributeModifier.Operation operation)
    {
        return as$temporaryInjections.getOrDefault(operation, Map.of()).values();
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void setAttrInstanceOwner(EntityType<? extends LivingEntity> entityType, Level level, CallbackInfo ci)
    {
        if (!DefaultAttributes.hasSupplier(entityType))
            return;
        for (var attr : ((AttributeSupplierDuckType) DefaultAttributes.getSupplier(entityType)).getAttributes())
        {
            var instance = getAttribute(attr);
            if (instance != null)
                ((EntityAttributeInstance)instance).setEntity((LivingEntity)(Object)this);
        }
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    public void as$tickInjections(CallbackInfo ci)
    {
        var injections = as$getInjections();
        if (injections.isEmpty())
            return;

        var tick = level().getGameTime();
        for (var injection : injections)
        {
            if (tick >= injection.endTick)
            {
                as$temporaryInjections.get(injection.modifier.getOperation()).remove(injection.modifier.getId());
                var attr = getAttribute(injection.attribute);
                if (attr != null)
                    ((EntityAttributeInstance)attr).publicSetDirty();
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At(value = "HEAD"))
    public void save(CompoundTag nbt, CallbackInfo ci) {
        nbt.putBoolean("ASLoaded", as$loaded);
        ListTag injectionsTag = new ListTag();
        for (var entry : as$getInjections()) {
            injectionsTag.add(entry.serialize());
        }
        nbt.put("ASInjections", injectionsTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "HEAD"))
    public void load(CompoundTag nbt, CallbackInfo ci) {
        if (nbt.contains("ASLoaded"))
            as$setLoaded();

        if (nbt.contains("ASInjections")) {
            ListTag injectionsTag = nbt.getList("ASInjections", Tag.TAG_COMPOUND);
            for (int i = 0; i < injectionsTag.size(); i++) {
                TemporaryAttributeInjection entry = TemporaryAttributeInjection.fromTag(injectionsTag.getCompound(i));
                if (entry.attribute != null)
                    as$addInjection(entry);
            }
        }
    }

    @Override
    public void as$addInjection(TemporaryAttributeInjection injection) {
        as$temporaryInjections.computeIfAbsent(injection.modifier.getOperation(), op -> new HashMap<>()).put(injection.modifier.getId(), injection);
        var attr = getAttribute(injection.attribute);
        if (attr != null)
            ((EntityAttributeInstance)attr).publicSetDirty();
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
