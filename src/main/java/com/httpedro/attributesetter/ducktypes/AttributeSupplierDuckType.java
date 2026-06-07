package com.httpedro.attributesetter.ducktypes;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.Collection;
import java.util.Map;

public interface AttributeSupplierDuckType {
    Collection<Holder<Attribute>> getAttributes();
}
