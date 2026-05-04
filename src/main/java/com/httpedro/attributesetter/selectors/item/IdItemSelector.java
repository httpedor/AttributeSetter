package com.httpedro.attributesetter.selectors.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class IdItemSelector extends ItemSelector {

    public ResourceLocation id;
    public IdItemSelector(ResourceLocation id)
    {
        this.id = id;
    }
    @Override
    protected boolean testImpl(ItemStack obj) {
        return BuiltInRegistries.ITEM.getKey(obj.getItem()).equals(id);
    }

    @Override
    public float getSpecificity() {
        return 50.0f;
    }
    
}

