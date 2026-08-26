package com.httpedro.attributesetter.setters.item;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

public class ItemMaxStackSetter extends ItemComponentsSetter{
    public int maxStackSize = 0;
    public float multiplier = 0;
    /** Added after the value/multiplier stage, so `x2 +8` means what it reads like. */
    public int offset = 0;

    public ItemMaxStackSetter(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }
    public ItemMaxStackSetter(float multiplier) {
        this.multiplier = multiplier;
    }

    public ItemMaxStackSetter offset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map) {
        // Nothing asked for at all means "put it back to the vanilla default".
        if (maxStackSize <= 0 && multiplier <= 0 && offset == 0)
        {
            builder.remove(DataComponents.MAX_STACK_SIZE);
            return;
        }

        Integer current = map.has(DataComponents.MAX_STACK_SIZE) ? map.get(DataComponents.MAX_STACK_SIZE) : null;
        int result;
        if (multiplier > 0 && current != null)
            result = (int) (current * multiplier);
        else if (maxStackSize > 0)
            result = maxStackSize;
        else if (current != null)
            result = current;
        else
            return; // offset only, on an item with no stack size to offset from

        // Vanilla refuses anything outside this range, and a stack of 0 would make the item unusable.
        builder.set(DataComponents.MAX_STACK_SIZE, Math.max(1, Math.min(99, result + offset)));
    }
}
