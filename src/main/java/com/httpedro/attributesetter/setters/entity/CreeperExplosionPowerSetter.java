package com.httpedro.attributesetter.setters.entity;

import com.httpedro.attributesetter.ducktypes.CreeperDuckType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;

public class CreeperExplosionPowerSetter extends EntitySetter {
    public int power;
    public float multiplier;
    public CreeperExplosionPowerSetter(int power)
    {
        this.power = power;
        this.multiplier = 1;
    }
    public CreeperExplosionPowerSetter(float multiplier)
    {
        this.multiplier = multiplier;
        this.power = 0;
    }
    @Override
    public void apply(LivingEntity target) {
        if (!(target instanceof Creeper creeper))
            return;

        var duckType = ((CreeperDuckType) creeper);
        if (power > 0)
            duckType.setExplosionPower(power);
        else
            duckType.setExplosionPower((int) (duckType.getExplosionPower() * multiplier));
    }
}
