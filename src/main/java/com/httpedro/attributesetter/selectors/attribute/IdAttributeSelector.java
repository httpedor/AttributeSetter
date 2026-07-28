package com.httpedro.attributesetter.selectors.attribute;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.ForgeRegistries;

public class IdAttributeSelector extends AttributeSelector {

    public ResourceLocation id;
    public IdAttributeSelector(ResourceLocation id)
    {
        this.id = id;
    }

    @Override
    protected boolean testImpl(Attribute obj) {
        var key = ForgeRegistries.ATTRIBUTES.getKey(obj);
        return key != null && key.equals(id);
    }

    @Override
    public float getSpecificity() {
        return 50.0f;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
