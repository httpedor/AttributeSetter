package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ASLivingEntity;
import com.httpedro.attributesetter.api.AttributeInjector;
import com.httpedro.attributesetter.ducktypes.EntityAttributeInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
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
    private static final WeakHashMap<LivingEntity, Set<AttributeInstance>> attributesetter$updated = new WeakHashMap<>();

    @Shadow
    public abstract Attribute getAttribute();

    @Shadow
    protected abstract Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation op);

    @Shadow
    protected abstract void setDirty();

    @Shadow
    @Final
    private Map<AttributeModifier.Operation, Set<AttributeModifier>> modifiersByOperation;

    @Inject(method = "getModifiersOrEmpty", at = @At("RETURN"), cancellable = true)
    private void addInjectedModifiers(AttributeModifier.Operation op, CallbackInfoReturnable<Collection<AttributeModifier>> cir) {
        var entity = getEntity();
        if (entity == null)
            return;
        var attrId = ForgeRegistries.ATTRIBUTES.getKey(getAttribute());
        if (attrId == null)
            return;

        Collection<AttributeModifier> modifiers = new LinkedList<>(cir.getReturnValue());
        modifiers.addAll(AttributeInjector.getInjectionsFor(attrId, op, entity));
        for (var entry : ((ASLivingEntity)entity).as$getInjections(op)) {
            var injAttrId = ForgeRegistries.ATTRIBUTES.getKey(entry.attribute);
            if (injAttrId != null && injAttrId.equals(attrId))
                modifiers.add(entry.modifier);
        }
        cir.setReturnValue(modifiers);
    }

    @Inject(method = "setDirty", at = @At("RETURN"))
    private void updateDependents(CallbackInfo ci)
    {
        var entity = getEntity();
        if (entity == null)
            return;
        var attrId = ForgeRegistries.ATTRIBUTES.getKey(getAttribute());
        if (attrId == null)
            return;

        var set = attributesetter$updated.computeIfAbsent(entity, e -> new HashSet<>());
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
            return this.modifiersByOperation.getOrDefault(operation, Set.of());
    }

    @Override
    public void publicSetDirty() {
        this.setDirty();
    }
}
