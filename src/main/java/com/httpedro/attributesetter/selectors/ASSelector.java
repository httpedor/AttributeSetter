package com.httpedro.attributesetter.selectors;

public abstract class ASSelector<T> {
    public boolean inverted = false;
    protected abstract boolean testImpl(T obj);

    public boolean canCache()
    {
        return false;
    }

    public boolean test(T obj)
    {
        var ret = testImpl(obj);
        return inverted ? !ret : ret;
    }

    public abstract float getSpecificity();
}
