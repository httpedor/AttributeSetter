package com.httpedro.attributesetter.mixin;

import com.httpedro.attributesetter.api.RemovalRegistry;
import com.httpedro.attributesetter.api.UniqueRegistry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Strips removed items out of every loot roll, and caps unique ones. All the public {@code getRandomItems}
 * overloads (and {@code fill}) funnel through this private one, so a single hook covers block drops, mob drops,
 * chest loot, fishing and gifts without having to rewrite the loot tables themselves.
 */
@Mixin(LootTable.class)
public class LootTableMixin {
    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;", at = @At("RETURN"))
    private void as$dropRemovedItems(LootContext context, CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        var items = cir.getReturnValue();
        if (items == null)
            return;

        if (RemovalRegistry.hasRemovedItems())
            items.removeIf(stack -> RemovalRegistry.isRemoved(stack.getItem()));

        // Unique cap: trim each rolled stack to whatever is still allowed, dropping the stack entirely when the
        // cap is already reached. Counting here (at generation) means loot that lands in a chest counts even
        // before a player takes it, which matches "only X will ever appear".
        if (UniqueRegistry.hasItemRules() && !items.isEmpty())
        {
            var level = context.getLevel();
            items.removeIf(stack -> {
                int allowed = UniqueRegistry.tryConsumeItem(stack.getItem(), stack.getCount(), level);
                if (allowed >= stack.getCount())
                    return false;
                if (allowed <= 0)
                    return true;
                stack.setCount(allowed);
                return false;
            });
        }
    }
}
