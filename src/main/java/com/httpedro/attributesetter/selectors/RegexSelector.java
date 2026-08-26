package com.httpedro.attributesetter.selectors;

import java.util.function.Function;
import java.util.regex.Pattern;

import net.minecraft.resources.ResourceLocation;

public class RegexSelector<T> extends ASSelector<T> {
    /** Which part of the id the pattern is matched against. */
    public enum Part {
        /** The whole {@code namespace:path}. */
        FULL,
        /** Just the path, so patterns don't have to spell out the namespace. */
        PATH,
        NAMESPACE
    }

    public String regex;
    public Pattern pattern;
    public Function<T, String> idExtractor;
    /** Which part of the id was matched. Kept so code that resolves selectors without an instance can redo it. */
    public Part part = Part.FULL;

    public RegexSelector(String regex, Function<T, String> idExtractor) {
        this(regex, idExtractor, false);
    }

    public RegexSelector(String regex, Function<T, String> idExtractor, boolean ignoreCase) {
        this.regex = regex;
        this.pattern = Pattern.compile(regex, ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
        this.idExtractor = idExtractor;
    }

    public static <T> RegexSelector<T> byId(String regex, Function<T, ResourceLocation> idExtractor) {
        return byId(regex, idExtractor, Part.FULL, false);
    }

    public static <T> RegexSelector<T> byId(String regex, Function<T, ResourceLocation> idExtractor, Part part, boolean ignoreCase) {
        var selector = new RegexSelector<T>(regex, obj -> {
            ResourceLocation id = idExtractor.apply(obj);
            if (id == null)
                return null;
            return switch (part) {
                case PATH -> id.getPath();
                case NAMESPACE -> id.getNamespace();
                default -> id.toString();
            };
        }, ignoreCase);
        selector.part = part;
        return selector;
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
        return id != null && pattern.matcher(id).matches();
	}
}
