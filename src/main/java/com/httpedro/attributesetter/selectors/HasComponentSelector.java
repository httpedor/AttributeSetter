package com.httpedro.attributesetter.selectors;

import com.httpedro.attributesetter.selectors.itemstack.ItemStackSelector;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;
import java.util.function.Function;

public class HasComponentSelector<T> extends ASSelector<T> {
    public DataComponentType<?> component;
	public Function<T, DataComponentMap> mapFunction;

    public HasComponentSelector(DataComponentType<?> component, Function<T, DataComponentMap> mapFunction) {
        this.component = component;
		this.mapFunction = mapFunction;
    }

	@Override
	protected boolean testImpl(T obj) {
        return mapFunction.apply(obj).has(component);
	}

	@Override
	public float getSpecificity() {
	    return 0f;
	}
}
