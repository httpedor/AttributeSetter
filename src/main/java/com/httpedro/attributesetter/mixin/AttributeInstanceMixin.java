package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ASLivingEntity;
import com.httpedro.attributesetter.api.AttributeInjector;
import com.httpedro.attributesetter.ducktypes.EntityAttributeInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceMixin implements EntityAttributeInstance {
    @Unique
    private LivingEntity attributesetter$owner;
    @Unique
    private static WeakHashMap<LivingEntity, Set<AttributeInstance>> updated = new WeakHashMap<>();

    @Shadow
    public abstract Holder<Attribute> getAttribute();

    @Shadow
    protected abstract Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation op);


    @Shadow
    protected abstract void setDirty();

    @Shadow
    @Final
    private Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation;

    @Inject(method = "getModifiersOrEmpty", at = @At("RETURN"), cancellable = true)
    private void addInjectedModifiers(AttributeModifier.Operation op, CallbackInfoReturnable<Collection<AttributeModifier>> cir) {
        Collection<AttributeModifier> modifiers = new LinkedList<>(cir.getReturnValue());
        var attrId = getAttribute().unwrapKey().get().location();
        var entity = getEntity();
        if (entity == null)
            return;

        modifiers.addAll(AttributeInjector.getInjectionsFor(attrId, op, entity));
        for (var entry : ((ASLivingEntity)entity).as$getInjections(op))
            if (entry.attribute.unwrapKey().get().location().equals(attrId))
                modifiers.add(entry.modifier);
        cir.setReturnValue(modifiers);
    }

    @Inject(method = "setDirty", at = @At("RETURN"))
    private void updateDependents(CallbackInfo ci)
    {
        var attrId = getAttribute().unwrapKey().get().location();
        var entity = getEntity();
        if (entity == null)
            return;

        var set = updated.computeIfAbsent(entity, e -> new HashSet<>());
        boolean started = set.isEmpty();
        set.add((AttributeInstance)(Object)this);
        for (var dependent : AttributeInjector.getAttributesDependentOn(attrId))
        {
            var attrInstance = entity.getAttribute(dependent);
            if (attrInstance == null || set.contains(attrInstance))
                continue;
            if (attrInstance instanceof EntityAttributeInstance eai)
                eai.publicSetDirty();
        }

        if (started)
        {
            set.clear();
        }
    }

    @Override
    public LivingEntity getEntity() {
        return attributesetter$owner;
    }

    @Override
    public void setEntity(LivingEntity entity) {
        this.attributesetter$owner = entity;
    }

    @Override
    public Collection<AttributeModifier> getModifiersOrEmptyExposed(AttributeModifier.Operation operation, boolean countingInjection) {
        if (countingInjection)
            return getModifiersOrEmpty(operation);
        else
            return this.modifiersByOperation.getOrDefault(operation, Map.of()).values();
    }

    @Override
    public void publicSetDirty() {
        this.setDirty();
    }
}
