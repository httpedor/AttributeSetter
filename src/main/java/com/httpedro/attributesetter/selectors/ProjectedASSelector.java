package com.httpedro.attributesetter.selectors;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Runs a selector written for a target type's parent against the derived type, by projecting the object first
 * (an {@code ItemStack} down to its {@code Item}, say). This is what makes selectors inherit: anything the
 * parent target type knows how to parse is automatically usable by everything derived from it.
 */
public class ProjectedASSelector<T, P> extends ASSelector<T> {
    public final ASSelector<P> inner;
    public final Function<T, P> projection;

    public ProjectedASSelector(ASSelector<P> inner, Function<T, P> projection) {
        this.inner = inner;
        this.projection = projection;
    }

    @Override
    protected boolean testImpl(T obj) {
        var projected = projection.apply(obj);
        return projected != null && inner.test(projected);
    }

    @Override
    public float getSpecificity() {
        return inner.specificity();
    }

    @Override
    public boolean canCache() {
        return inner.canCache();
    }

    @Override
    public Collection<ASSelector<T>> children() {
        return List.of();
    }

    /** The wrapped selector - {@link #children()} can't expose it, since it has a different target type. */
    public ASSelector<P> unwrap() {
        return inner;
    }

    @Override
    public <S> S find(Class<S> type) {
        if (type.isInstance(this))
            return type.cast(this);
        return inner.find(type);
    }
}
