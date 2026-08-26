package com.httpedro.attributesetter.selectors;

/** Matches everything. Written as {@code "*"}, or as {@code {"type": "always"}}. */
public class AlwaysSelector<T> extends ASSelector<T> {
    @Override
    protected boolean testImpl(T obj) {
        return true;
    }

    @Override
    public float getSpecificity() {
        // Deliberately below every other selector, so a blanket rule never overrides a targeted one.
        return -100;
    }

    @Override
    public boolean canCache() {
        return true;
    }
}
