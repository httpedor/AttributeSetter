package com.httpedor.attributesetter.selectors.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class IdEntitySelector extends EntitySelector {
    public Identifier id;

    public IdEntitySelector(Identifier id)
    {
        this.id = id;
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return Registries.ENTITY_TYPE.getId(obj.getType()).equals(id);
    }

    @Override
    public float getSpecificity() {
        return 50.0f;
    }
}