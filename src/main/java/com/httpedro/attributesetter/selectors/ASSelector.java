package com.httpedro.attributesetter.selectors;

import java.util.Collection;
import java.util.List;

public abstract class ASSelector<T> {
    public boolean inverted = false;
    /**
     * Set from a selector object's {@code specificity} field. Entries are applied least-specific first, so
     * raising this makes an entry win over the ones it overlaps with, whatever kind of selector it is.
     */
    public Float specificityOverride = null;

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

    /** The selector's natural specificity. Use {@link #specificity()} when ordering entries. */
    public abstract float getSpecificity();

    /** The specificity actually used for ordering: the author's override when there is one. */
    public final float specificity()
    {
        return specificityOverride != null ? specificityOverride : getSpecificity();
    }

    /** The selectors this one wraps (composites, projections). Used to look inside a selector tree. */
    public Collection<ASSelector<T>> children()
    {
        return List.of();
    }

    /**
     * Finds the first selector of the given class in this tree, so a setter can ask questions like "which item
     * is this entry about?" without caring how the selector was written.
     */
    public <S> S find(Class<S> type)
    {
        if (type.isInstance(this))
            return type.cast(this);
        for (var child : children())
        {
            var found = child.find(type);
            if (found != null)
                return found;
        }
        return null;
    }
}
