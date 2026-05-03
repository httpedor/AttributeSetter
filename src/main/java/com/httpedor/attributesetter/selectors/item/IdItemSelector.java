package com.httpedor.attributesetter.selectors.item;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class IdItemSelector extends ItemSelector {
    public Identifier id;

    public IdItemSelector(Identifier id)
    {
        this.id = id;
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        return Registries.ITEM.getId(obj.getItem()).equals(id);
    }

    @Override
    public float getSpecificity() {
        return 50.0f;
    }
}