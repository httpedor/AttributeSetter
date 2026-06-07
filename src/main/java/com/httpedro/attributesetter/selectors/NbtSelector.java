package com.httpedro.attributesetter.selectors;

import java.util.function.Function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;

public class NbtSelector<T> extends ASSelector<T> {
    public CompoundTag nbt;
    public Function<T, CompoundTag> nbtExtractor;
    public NbtSelector(CompoundTag nbt, Function<T, CompoundTag> nbtExtractor)
    {
        this.nbt = nbt;
        this.nbtExtractor = nbtExtractor;
    }
    public NbtSelector(String nbtStr, Function<T, CompoundTag> nbtExtractor) throws CommandSyntaxException
    {
        this.nbt = new TagParser(new StringReader(nbtStr)).readStruct();
        this.nbtExtractor = nbtExtractor;
    }

    @Override
    protected boolean testImpl(T obj) {
        CompoundTag objNbt = nbtExtractor.apply(obj);
        return NbtUtils.compareNbt(this.nbt, objNbt, true);
    }

    @Override
    public float getSpecificity() {
        return 100;
    }
}
