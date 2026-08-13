package com.httpedro.attributesetter.api;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-save, monotonic counters for the {@code make_unique} setters. Each key is a string built by
 * {@link UniqueRegistry} ({@code "item:<id>"}, {@code "entity:<id>"} or {@code "group:<id>"}), mapped to how many
 * copies have ever been created in this save.
 *
 * <p>Unlike the datapack-driven caps (which are rebuilt every reload), these counts live with the world so a
 * unique item stays unique across restarts. They are attached to the overworld's data storage, which makes them
 * genuinely per-save rather than per-dimension.
 */
public class UniqueCounts extends SavedData {
    public static final String NAME = "attributesetter_unique";

    private final Map<String, Integer> counts = new HashMap<>();

    public static SavedData.Factory<UniqueCounts> factory()
    {
        return new SavedData.Factory<>(UniqueCounts::new, UniqueCounts::load, null);
    }

    public static UniqueCounts get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    public static UniqueCounts load(CompoundTag tag, HolderLookup.Provider registries)
    {
        UniqueCounts data = new UniqueCounts();
        for (String key : tag.getAllKeys())
            data.counts.put(key, tag.getInt(key));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        for (var entry : counts.entrySet())
            tag.putInt(entry.getKey(), entry.getValue());
        return tag;
    }

    public int getCount(String key)
    {
        return counts.getOrDefault(key, 0);
    }

    public void setCount(String key, int value)
    {
        int clamped = Math.max(0, value);
        Integer previous = counts.put(key, clamped);
        if (previous == null || previous != clamped)
            setDirty();
    }
}
