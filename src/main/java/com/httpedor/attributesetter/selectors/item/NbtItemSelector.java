package com.httpedor.attributesetter.selectors.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;

public class NbtItemSelector extends ItemSelector {
    public NbtCompound nbt;

    public NbtItemSelector(NbtCompound nbt) {
        this.nbt = nbt;
    }

    public NbtItemSelector(String nbtString) throws Exception {
        if (nbtString != null)
            nbt = StringNbtReader.parse(nbtString);
    }

    @Override
    protected boolean testImpl(ItemStack obj) {
        if (nbt == null)
            return true;
        NbtCompound itemNbt = new NbtCompound();
        obj.writeNbt(itemNbt);
        return NbtHelper.matches(nbt, itemNbt, true);
    }

    @Override
    public float getSpecificity() {
        return 100f;
    }
}