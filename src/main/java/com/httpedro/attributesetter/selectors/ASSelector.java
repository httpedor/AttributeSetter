package com.httpedro.attributesetter.selectors;

public abstract class ASSelector<T> {
    public boolean inverted = false;
    protected abstract boolean testImpl(T obj);

    /**
     * Whether the result of this selector depends only on the type of the object (Item/EntityType) and not on
     * its per-instance state (NBT, enchantments, display name, ...). Cacheable selectors let the API cache the
     * computed setter list per Item/EntityType instead of re-testing every selector on every access.
     */
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