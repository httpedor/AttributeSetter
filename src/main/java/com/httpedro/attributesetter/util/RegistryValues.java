package com.httpedro.attributesetter.util;

import net.minecraft.core.Registry;

import java.util.Map;

public final class RegistryValues
{
    private RegistryValues() {}

    /**
     * Every value in a registry, safe to iterate after ids have been remapped.
     * <p>
     * {@link Registry#iterator()} walks {@code byId}, and NeoForge's {@code registerIdMapping} pads that list with
     * nulls when a remap leaves the id space sparse (registry sync on world join does exactly that). Guava's
     * transforming iterator then NPEs inside {@code next()}, so the holes cannot be skipped by the caller either.
     * {@link Registry#entrySet()} is backed by {@code byKey} instead, which never has holes.
     */
    public static <T> Iterable<T> of(Registry<T> registry)
    {
        return () -> registry.entrySet().stream().map(Map.Entry::getValue).iterator();
    }
}
