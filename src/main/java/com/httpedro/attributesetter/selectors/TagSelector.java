package com.httpedro.attributesetter.selectors;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class TagSelector<TInst, TSing> extends ASSelector<TInst> {
    TagKey<TSing> tagKey;
    public ResourceLocation tag;
    public Registry<TSing> registry;
    public Function<TInst, TSing> singletonGetter;

    public TagSelector(ResourceLocation tag, Registry<TSing> registry, Function<TInst, TSing> singletonGetter) {
        this.tag = tag;
        this.registry = registry;
        tagKey = TagKey.create(registry.key(), tag);
        this.singletonGetter = singletonGetter;
    }

	@Override
	protected boolean testImpl(TInst obj) {
        var tag = registry.getTag(tagKey);
        if (tag.isEmpty())
            return false;
        var key = registry.getKeyOrNull(singletonGetter.apply(obj));
        if (key == null)
            return false;
        var holder = registry.getHolder(key);
        if (holder.isEmpty())
            return false;

	    return tag.get().contains(holder.get());
	}

	@Override
	public float getSpecificity() {
	    return 0;
	}

	@Override
    public boolean canCache() {
        return true;
    }
}
