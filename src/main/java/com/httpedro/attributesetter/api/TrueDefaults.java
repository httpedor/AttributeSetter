package com.httpedro.attributesetter.api;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class TrueDefaults {
    private static Map<Item, DataComponentMap> trueDefaults;

    public static void populate()
    {
        trueDefaults = new HashMap<>();
        for (var item : BuiltInRegistries.ITEM)
        {
            trueDefaults.put(item, item.components());
        }
    }

    public static boolean isPopulated() {
        return trueDefaults != null;
    }

    public static DataComponentMap get(Item item) {
        if (!isPopulated())
            throw new IllegalStateException("TrueDefaults not populated yet");
        return trueDefaults.get(item);
    }
}
