package com.httpedro.attributesetter.selectors.entity;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.LivingEntity;

public class NbtEntitySelector extends EntitySelector {
    public CompoundTag nbt;
    public NbtEntitySelector(CompoundTag nbt)
    {
        this.nbt = nbt;
    }
    public NbtEntitySelector(String nbtStr) throws CommandSyntaxException
    {
        this.nbt = new TagParser(new StringReader(nbtStr)).readStruct();
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        CompoundTag entityNbt = new CompoundTag();
        obj.saveWithoutId(entityNbt);
        return NbtUtils.compareNbt(this.nbt, entityNbt, true);
    }
    
    @Override
    public float getSpecificity() {
        return 100;
    }
}
