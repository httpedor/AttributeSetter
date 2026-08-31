package com.httpedro.attributesetter.compat.curios;

import com.httpedro.attributesetter.setters.ASEventSetter;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * Wraps the setter the ordinary (non-curio) builders produced and only lets it through for stacks that turn out
 * not to be curios. It exists for the one case {@link CuriosCompat} cannot decide at reload time - Curios not
 * having loaded its slots yet - where the entry is registered both ways and the decision is made per event,
 * by which point Curios is always ready.
 */
public class NonCurioSetter<TEvent extends Event> extends ASEventSetter<ItemStack, TEvent> {

    private final ASEventSetter<ItemStack, TEvent> delegate;

    public NonCurioSetter(ASEventSetter<ItemStack, TEvent> delegate) {
        super(delegate.getEventClass());
        this.delegate = delegate;
    }

    @Override
    public void apply(TEvent event) {
        var stack = delegate.getTarget(event);
        if (stack == null || !CuriosCompat.slotsFor(stack).isEmpty())
            return;
        delegate.apply(event);
    }

    @Override
    public ItemStack getTarget(TEvent event) {
        return delegate.getTarget(event);
    }
}
