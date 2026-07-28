package com.httpedro.attributesetter.api;

import com.httpedro.attributesetter.ducktypes.ItemDuckType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Because item-global setters (max stack, durability, food) mutate the shared {@link Item} in place, we snapshot
 * each item's original values the first time it is touched so that a datapack {@code /reload} can restore items
 * whose entries were removed, instead of leaving stale modifications behind.
 */
public class TrueDefaults {
    private static final class Snapshot {
        final int maxStackSize;
        final int maxDamage;
        final FoodProperties foodProperties;
        final Integer eatTicks;
        final ItemStack convertsTo;

        Snapshot(int maxStackSize, int maxDamage, FoodProperties foodProperties, Integer eatTicks, ItemStack convertsTo) {
            this.maxStackSize = maxStackSize;
            this.maxDamage = maxDamage;
            this.foodProperties = foodProperties;
            this.eatTicks = eatTicks;
            this.convertsTo = convertsTo;
        }
    }

    private static final Map<Item, Snapshot> defaults = new HashMap<>();

    /** Capture the original state of an item, once, before it is first mutated. */
    public static void snapshot(Item item) {
        if (defaults.containsKey(item))
            return;
        var duck = (ItemDuckType) item;
        defaults.put(item, new Snapshot(
                duck.as$getMaxStackSize(),
                item.maxDamage,
                duck.as$getFoodProperties(),
                duck.as$getEatTicks(),
                duck.as$getConvertsTo()
        ));
    }

    /** Restore every mutated item to its captured original state and forget the snapshots. */
    public static void restoreAll() {
        for (var entry : defaults.entrySet()) {
            var item = entry.getKey();
            var snap = entry.getValue();
            var duck = (ItemDuckType) item;
            duck.as$setMaxStackSize(snap.maxStackSize);
            item.maxDamage = snap.maxDamage;
            duck.as$setFoodProperties(snap.foodProperties);
            duck.as$setEatTicks(snap.eatTicks);
            duck.as$setConvertsTo(snap.convertsTo);
        }
        defaults.clear();
    }
}
