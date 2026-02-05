package com.httpedro.attributesetter.selectors;

import java.util.Collection;

public class CompositeASSelector<T> extends ASSelector<T> {
    public enum Mode {
        AND,
        OR
    }
    public Mode mode = Mode.AND;
    public ASSelector<T>[] selectors;

    public CompositeASSelector(ASSelector<T>[] selectors, Mode mode) {
        this.selectors = selectors;
        this.mode = mode;
    }
    public CompositeASSelector(ASSelector<T>[] selectors) {
        this.selectors = selectors;
    }
    public CompositeASSelector(Collection<ASSelector<T>> selectors, Mode mode) {
        this.selectors = selectors.toArray(new ASSelector[0]);
        this.mode = mode;
    }

    @Override
    protected boolean testImpl(T obj) {
        switch (mode) {
            case AND:
                for (var selector : selectors) {
                    if (!selector.test(obj))
                        return false;
                }
                return true;
            case OR:
                for (var selector : selectors) {
                    if (selector.test(obj))
                        return true;
                }
                return false;
            default:
                return false;
        }
    }
    @Override
    public float getSpecificity() {
        float biggestPrio = Float.NEGATIVE_INFINITY;
        for (var selector : selectors) {
            var prio = selector.getSpecificity();
            if (prio > biggestPrio)
                biggestPrio = prio;
        }
        return biggestPrio;
    }
    
}
