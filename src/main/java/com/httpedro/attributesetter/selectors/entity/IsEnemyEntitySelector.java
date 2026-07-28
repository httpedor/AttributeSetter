package com.httpedro.attributesetter.selectors.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

public class IsEnemyEntitySelector extends EntitySelector {

    @Override
    protected boolean testImpl(LivingEntity obj) {
        return obj instanceof Enemy;
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
