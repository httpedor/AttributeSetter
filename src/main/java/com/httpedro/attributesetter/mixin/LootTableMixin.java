package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.api.RemovalRegistry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Strips removed items out of every loot roll. Forge funnels all the public {@code getRandomItems} overloads (and
 * {@code fill}) through this private one, so a single hook covers block drops, mob drops, chest loot, fishing and
 * gifts without having to rewrite the loot tables themselves.
 */
@Mixin(LootTable.class)
public class LootTableMixin {
    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private void as$dropRemovedItems(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (!RemovalRegistry.hasRemovedItems())
            return;
        var items = cir.getReturnValue();
        if (items != null)
            items.removeIf(stack -> RemovalRegistry.isRemoved(stack.getItem()));
    }
}
