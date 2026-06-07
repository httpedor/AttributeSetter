package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.ducktypes.CreeperDuckType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Creeper.class)
public class CreeperMixin extends Mob implements CreeperDuckType {

    @Shadow
    private int explosionRadius;

    protected CreeperMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void setExplosionPower(int power) {
        explosionRadius = power;
    }

    @Override
    public int getExplosionPower() {
        return explosionRadius;
    }
}
