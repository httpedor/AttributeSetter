package com.httpedro.attributesetter.setters;

public abstract class ASSetter<T> {
    public abstract void apply(T target);

    public boolean shouldApply(T target) {
        return true;
    }
}
