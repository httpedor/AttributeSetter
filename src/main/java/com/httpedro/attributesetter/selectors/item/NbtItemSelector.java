package com.httpedro.attributesetter.selectors.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

public class NbtItemSelector extends ItemSelector{

    public CompoundTag nbt;
    public NbtItemSelector(CompoundTag nbt) {
        this.nbt = nbt;
    }
    public NbtItemSelector(String nbtString) throws CommandSyntaxException {
        if (nbtString != null)
            nbt = new TagParser(new StringReader(nbtString)).readStruct();
    }
    @Override
    protected boolean testImpl(ItemStack obj) {
        if (nbt == null)
            return true;
        
        return NbtUtils.compareNbt(nbt, obj.save(new CompoundTag()), true);
    }

    @Override
    public float getSpecificity() {
        return 100f;
    }
    
}
