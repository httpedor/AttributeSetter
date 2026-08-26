package com.httpedro.attributesetter.setters.itemstack.attribute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.httpedro.attributesetter.setters.ASEventSetter;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public abstract class ItemStackAttributeSetter extends ASEventSetter<ItemStack, ItemAttributeModifierEvent> {
    /**
     * Items this setter was already folded into at reload time, mapped to the result of that fold. See
     * {@link ItemAttributeBaker}. Written once per reload, read from both logical sides during the event.
     */
    private final Map<Item, ItemAttributeModifiers> baked = new ConcurrentHashMap<>();

    protected ItemStackAttributeSetter() {
		super(ItemAttributeModifierEvent.class);
	}

	@Override
    public ItemStack getTarget(ItemAttributeModifierEvent event) {
        return event.getItemStack();
    }

    void markBaked(Item item, ItemAttributeModifiers result) {
        baked.put(item, result);
    }

    @Override
    public final void apply(ItemAttributeModifierEvent e) {
        // Skip only when this stack is actually running on the baked defaults. A stack carrying its own
        // ATTRIBUTE_MODIFIERS component (another mod, a loot table, /give ...) never went through the bake, so
        // it still needs the setter applied live.
        var bakedResult = baked.get(e.getItemStack().getItem());
        if (bakedResult != null && bakedResult.equals(e.getDefaultModifiers()))
            return;
        applyModifiers(e);
    }

    protected abstract void applyModifiers(ItemAttributeModifierEvent e);
}
