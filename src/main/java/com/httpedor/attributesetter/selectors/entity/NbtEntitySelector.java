package com.httpedor.attributesetter.selectors.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;

public class NbtEntitySelector extends EntitySelector {
    public NbtCompound nbt;

    public NbtEntitySelector(NbtCompound nbt)
    {
        this.nbt = nbt;
    }

    public NbtEntitySelector(String nbtStr) throws Exception
    {
        this.nbt = StringNbtReader.parse(nbtStr);
    }

    @Override
    protected boolean testImpl(LivingEntity obj) {
        NbtCompound entityNbt = new NbtCompound();
        obj.writeNbt(entityNbt);
        return NbtHelper.matches(this.nbt, entityNbt, true);
    }

    @Override
    public float getSpecificity() {
        return 100;
    }
}