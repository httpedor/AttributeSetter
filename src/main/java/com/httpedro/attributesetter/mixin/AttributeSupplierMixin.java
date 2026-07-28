package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ducktypes.AttributeSupplierDuckType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.Map;

@Mixin(AttributeSupplier.class)
public class AttributeSupplierMixin implements AttributeSupplierDuckType {
    @Shadow
    @Final
    private Map<Attribute, AttributeInstance> instances;

    @Override
    public Collection<Attribute> getAttributes() {
        return instances.keySet();
    }
}
