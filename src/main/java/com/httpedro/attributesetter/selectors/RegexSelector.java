package com.httpedro.attributesetter.selectors;

import java.util.function.Function;

import net.minecraft.resources.ResourceLocation;

public class RegexSelector<T> extends ASSelector<T> {
    public String regex;
    public Function<T, String> idExtractor;
    public RegexSelector(String regex, Function<T, String> idExtractor) {
        this.regex = regex;
        this.idExtractor = idExtractor;
    }

    public static <T> RegexSelector<T> byId(String regex, Function<T, ResourceLocation> idExtractor) {
        return new RegexSelector<>(regex, obj -> {
            ResourceLocation id = idExtractor.apply(obj);
            return id != null ? id.toString() : null;
        });
    }

    @Override
    public float getSpecificity() {
        return 25;
    }

    @Override
    public boolean canCache() {
        return true;
    }

	@Override
	protected boolean testImpl(T obj) {
        String id = idExtractor.apply(obj);
        return id != null && id.matches(regex);
	}
}
