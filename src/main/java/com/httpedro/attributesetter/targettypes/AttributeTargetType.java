package com.httpedro.attributesetter.targettypes;

import com.httpedro.attributesetter.targettypes.interfaces.IIdentifiableTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IRegistryAssociatedTargetType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class AttributeTargetType extends SingletonTargetType<Attribute> implements IIdentifiableTargetType<Attribute>,
        IRegistryAssociatedTargetType<Attribute> {
    public AttributeTargetType() {
        super(Attribute.class);
    }

    @Override
    public String getFolderName() {
        return "attribute";
    }

    @Override
    public java.util.List<String> getFolderAliases() {
        return java.util.List.of("attributes");
    }

    @Override
    public ResourceLocation getId(Attribute obj) {
        return BuiltInRegistries.ATTRIBUTE.getKey(obj);
    }

    @Override
    public Registry<Attribute> getRegistry() {
        return BuiltInRegistries.ATTRIBUTE;
    }
}
