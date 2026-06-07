package com.httpedro.attributesetter.setters;

import net.neoforged.bus.api.Event;

public abstract class ASEventSetter<T, TEvent extends Event> extends ASSetter<T>
{
    protected final Class<TEvent> eventClass;

    protected ASEventSetter(Class<TEvent> eventClass) {
        this.eventClass = eventClass;
    }
    @Override
    public void apply(T target) {
        throw new UnsupportedOperationException("ASEventSetters cannot be applied directly, they must be applied through their event handlers");
    }

    public abstract void apply(TEvent event);

    /**
    * Used so that the selectors can check if this event should be applied or not
    * @param event
    * @return
 */
    public abstract T getTarget(TEvent event);

    public Class<TEvent> getEventClass() {
        return eventClass;
    }
}
