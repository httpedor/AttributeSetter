package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.api.TrueDefaults;
import com.httpedro.attributesetter.ducktypes.ItemDuckType;

import net.minecraft.world.item.ItemStack;

public class ItemMaxStackSetter extends ItemSetter {
    public int maxStackSize = 0;
    public float multiplier = 0;

    public ItemMaxStackSetter(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }
    public ItemMaxStackSetter(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public boolean isGlobal() {
        return true;
    }

    @Override
    public void apply(ItemStack target) {
        var item = target.getItem();
        TrueDefaults.snapshot(item);
        var duck = (ItemDuckType) item;
        if (multiplier > 0) {
            duck.as$setMaxStackSize(Math.max(1, (int) (duck.as$getMaxStackSize() * multiplier)));
            return;
        }
        if (maxStackSize > 0)
            duck.as$setMaxStackSize(maxStackSize);
    }
}
