package com.httpedor.attributesetter.selectors;

public abstract class ASSelector<T> {
    public boolean inverted = false;

    protected abstract boolean testImpl(T obj);

    public boolean test(T obj)
    {
        var ret = testImpl(obj);
        return inverted ? !ret : ret;
    }

    public abstract float getSpecificity();
}