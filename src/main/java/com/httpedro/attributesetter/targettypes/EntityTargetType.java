package com.httpedro.attributesetter.targettypes;

import java.util.function.Function;

import com.httpedro.attributesetter.targettypes.interfaces.INBTSerializableTargetType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class EntityTargetType extends RegistryTargetType<LivingEntity, EntityType<?>> implements INBTSerializableTargetType<LivingEntity> {
    public EntityTargetType() {
        super(LivingEntity.class, BuiltInRegistries.ENTITY_TYPE);
    }

	@Override
	public String getFolderName() {
	    return "entity";
	}

	@Override
    public EntityType<?> getSingleton(LivingEntity instance) {
        return instance.getType();
    }

	@Override
	public Function<LivingEntity, Tag> getSerializer() {
	    return (entity) -> {
            return entity.saveWithoutId(new CompoundTag());
        };
	}

}
