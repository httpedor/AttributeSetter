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
    public ItemDurabilitySetter(int value) {
        this.value = value;
    }
    public ItemDurabilitySetter(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map) {
        if (value <= 0 && multiplier <= 0)
        {
            builder.remove(DataComponents.MAX_DAMAGE);
            builder.remove(DataComponents.DAMAGE);
            return;
        }
        if (multiplier > 0 && map.has(DataComponents.MAX_DAMAGE))
        {
            int durability = (int) (map.get(DataComponents.MAX_DAMAGE) * multiplier);
            builder.set(DataComponents.MAX_DAMAGE, durability);
            return;
        }
        if (value > 0)
            builder.set(DataComponents.MAX_DAMAGE, value);
    }
}
