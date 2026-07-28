package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

public class MobCategoryEntitySelector extends EntitySelector {
    public MobCategory category;

    public MobCategoryEntitySelector(MobCategory category) {
        this.category = category;
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return obj.getType().getCategory() == category;
    }

    @Override
    protected Boolean testTypeImpl(EntityType<?> type) {
        return type.getCategory() == category;
    }

    @Override
    public float getSpecificity() {
        return 0;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
