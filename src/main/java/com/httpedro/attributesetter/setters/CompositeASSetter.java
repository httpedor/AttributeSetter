package com.httpedro.attributesetter.setters;

public class CompositeASSetter<T> extends ASSetter<T>{
    public ASSetter<T>[] setters;
    public CompositeASSetter(ASSetter<T>[] setters) {
        this.setters = setters;
    }

    @Override
    public void apply(T target) {
        for (var setter : setters)
            setter.apply(target);
    }
    
}
