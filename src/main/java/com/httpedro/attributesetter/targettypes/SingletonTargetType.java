package com.httpedro.attributesetter.targettypes;

import com.httpedro.attributesetter.api.TargetType;

public abstract class SingletonTargetType<T> extends TargetType<T, T> {
    protected SingletonTargetType(Class<T> typeClass) {
        super(typeClass);
    }

    @Override
    public T getCacheKey(T object) {
        return object;
    }
}
