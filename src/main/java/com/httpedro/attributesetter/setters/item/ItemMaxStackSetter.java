package com.httpedro.attributesetter.setters.item;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;

public class ItemMaxStackSetter extends ItemComponentsSetter{
    public int maxStackSize = 0;
    public float multiplier = 0;
    public ItemMaxStackSetter(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }
    public ItemMaxStackSetter(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map) {
        if (maxStackSize <= 0 && multiplier <= 0)
        {
            builder.remove(DataComponents.MAX_STACK_SIZE);
            return;
        }
        if (multiplier > 0 && map.has(DataComponents.MAX_STACK_SIZE))
        {
            int durability = (int) (map.get(DataComponents.MAX_STACK_SIZE) * multiplier);
            builder.set(DataComponents.MAX_STACK_SIZE, durability);
            return;
        }
        if (maxStackSize > 0)
            builder.set(DataComponents.MAX_STACK_SIZE, maxStackSize);
    }
}
