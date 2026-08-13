package com.httpedro.attributesetter.setters.itemstack;

import com.httpedro.attributesetter.api.RemovalRegistry;

import net.minecraft.world.item.ItemStack;

/**
 * The {@code item}-folder counterpart of {@link com.httpedro.attributesetter.setters.item.ItemRemoveSetter}: same
 * effect, but lives on the itemstack target type so a {@code remove} entry works in the folder people actually use
 * for items. It only records the stack's item; the removal handlers do the rest.
 *
 * <p>Not an event setter, so it is never applied per stack - it is collected once at reload time by probing every
 * item (see {@code DataReloader.load}).
 */
public class ItemStackRemoveSetter extends ItemStackSetter {
    @Override
    public void apply(ItemStack target) {
        RemovalRegistry.removeItem(target.getItem());
    }
}
