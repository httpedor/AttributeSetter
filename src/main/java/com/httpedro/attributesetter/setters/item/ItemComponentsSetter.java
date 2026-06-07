package com.httpedro.attributesetter.setters.item;

import com.httpedro.attributesetter.api.TrueDefaults;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;

public abstract class ItemComponentsSetter extends ItemSetter {
    public abstract void applyPatch(DataComponentPatch.Builder builder, DataComponentMap map);

    @Override
    public void apply(Item target) {
        var builder = DataComponentPatch.builder();
        applyPatch(builder, TrueDefaults.get(target));
        target.modifyDefaultComponentsFrom(builder.build());
    }
}
