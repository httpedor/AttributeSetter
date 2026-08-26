package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.setters.itemstack.ItemStackSetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilitySetter extends ItemComponentsSetter {

    public int value = 0;
    public float multiplier = 0;
    /** Added after the value/multiplier stage, so `x2 +100` means what it reads like. */
    public int offset = 0;

    public ItemDurabilitySetter(int value) {
        this.value = value;
    }
    public ItemDurabilitySetter(float multiplier) {
        this.multiplier = multiplier;
    }

    public ItemDurabilitySetter offset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map) {
        // Nothing asked for at all means "make it unbreakable-by-absence", i.e. drop the durability entirely.
        if (value <= 0 && multiplier <= 0 && offset == 0)
        {
            builder.remove(DataComponents.MAX_DAMAGE);
            builder.remove(DataComponents.DAMAGE);
            return;
        }

        Integer current = map.has(DataComponents.MAX_DAMAGE) ? map.get(DataComponents.MAX_DAMAGE) : null;
        int result;
        if (multiplier > 0 && current != null)
            result = (int) (current * multiplier);
        else if (value > 0)
            result = value;
        else if (current != null)
            result = current;
        else
            return; // offset only, on an item that has no durability to offset from

        builder.set(DataComponents.MAX_DAMAGE, Math.max(1, result + offset));
    }
}
