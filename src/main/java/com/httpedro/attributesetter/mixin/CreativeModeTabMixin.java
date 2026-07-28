package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.api.RemovalRegistry;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Hides removed items from the creative inventory and from the creative search tab. Filtering on read rather than
 * inside {@code buildContents} means a {@code /reload} takes effect without having to force the tabs to rebuild,
 * and JEI - which builds its item list from the creative tab contents - picks the change up too.
 */
@Mixin(CreativeModeTab.class)
public class CreativeModeTabMixin {
    @Inject(method = "getDisplayItems", at = @At("RETURN"), cancellable = true)
    private void as$filterDisplayItems(CallbackInfoReturnable<Collection<ItemStack>> cir) {
        cir.setReturnValue(as$filter(cir.getReturnValue()));
    }

    @Inject(method = "getSearchTabDisplayItems", at = @At("RETURN"), cancellable = true)
    private void as$filterSearchItems(CallbackInfoReturnable<Collection<ItemStack>> cir) {
        cir.setReturnValue(as$filter(cir.getReturnValue()));
    }

    private static Collection<ItemStack> as$filter(Collection<ItemStack> items) {
        if (items == null || items.isEmpty() || !RemovalRegistry.hasRemovedItems())
            return items;

        Collection<ItemStack> filtered = new ArrayList<>(items.size());
        for (var stack : items)
        {
            if (!RemovalRegistry.isRemoved(stack.getItem()))
                filtered.add(stack);
        }
        return filtered;
    }
}
