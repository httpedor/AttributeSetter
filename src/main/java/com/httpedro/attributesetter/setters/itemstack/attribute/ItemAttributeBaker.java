package com.httpedro.attributesetter.setters.itemstack.attribute;

import java.util.ArrayList;
import java.util.List;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.TargetTypes;
import com.httpedro.attributesetter.api.TrueDefaults;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * Folds the item-wide attribute setters into each item's default ATTRIBUTE_MODIFIERS component at reload time.
 * <p>
 * ItemAttributeModifierEvent only reaches code that goes through {@code ItemStack#getAttributeModifiers()}. Plenty
 * of mods read DataComponents.ATTRIBUTE_MODIFIERS straight off the stack instead, and they then see the unmodified
 * vanilla values. Better Combat is the worst case: its off-hand attack swap reads the raw component and does
 * remove-then-add against the player's attribute map, so it re-adds the vanilla {@code minecraft:base_attack_damage}
 * that a `base` setter had removed - on top of ours, and without ever taking it back off. Baking makes the component
 * and the event agree, so those readers see the same numbers everyone else does.
 * <p>
 * Only cacheable selectors (id/tag/regex - everything that depends on the item alone) can be baked. Selectors that
 * look at stack state stay on the event, where they belong.
 */
public class ItemAttributeBaker {
    private ItemAttributeBaker() {}

    public static void bake(Item item)
    {
        var entries = TargetTypes.ITEMSTACK.getCacheableEntriesForEvent(ItemAttributeModifierEvent.class);
        if (entries.isEmpty())
            return;

        var probe = new ItemStack(item);
        List<ItemStackAttributeSetter> matched = new ArrayList<>();
        for (var entry : entries)
        {
            if (!entry.selector.test(probe))
                continue;
            for (var setter : entry.setters)
            {
                if (setter instanceof ItemStackAttributeSetter attributeSetter)
                    matched.add(attributeSetter);
            }
        }
        if (matched.isEmpty())
            return;

        // The defaults the event would have started from, taken from TrueDefaults so a previous reload's bake
        // can never be folded in twice.
        var defaults = TrueDefaults.get(item).getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        var event = new ItemAttributeModifierEvent(probe, defaults);
        for (var setter : matched)
        {
            try {
                setter.applyModifiers(event);
            } catch (Exception ex) {
                Attributesetter.LOGGER.error("Error while baking setter '{}' into item '{}':", setter.getClass().getName(), item, ex);
            }
        }

        var result = event.build();
        if (result.equals(defaults))
            return;

        item.modifyDefaultComponentsFrom(DataComponentPatch.builder().set(DataComponents.ATTRIBUTE_MODIFIERS, result).build());
        for (var setter : matched)
            setter.markBaked(item, result);
    }
}
