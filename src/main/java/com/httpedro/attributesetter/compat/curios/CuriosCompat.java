package com.httpedro.attributesetter.compat.curios;

import com.httpedro.attributesetter.Attributesetter;
import com.httpedro.attributesetter.TargetTypes;
import com.httpedro.attributesetter.selectors.IdSelector;
import com.httpedro.attributesetter.setters.ASSetter;
import com.httpedro.attributesetter.setters.CompositeASSetter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLLoader;
import top.theillusivec4.curios.api.CuriosApi;

public class CuriosCompat {

    public void bootstrap()
    {
        TargetTypes.ITEMSTACK.registerSetterBuilder(5, (obj, id, selector) -> {
            var opEl = obj.get("operation");
            var slotEl = obj.get("slot");
            // Every curio setter is about an attribute; bailing here keeps this builder from claiming - or
            // complaining about - the durability/food/tooltip entries that pass through on their way elsewhere.
            var attrEl = obj.get("attribute");
            if (attrEl == null)
                return null;
            String[] slots;
            if (slotEl == null)
            {
                // This is not working because of load order. The Curios mod loads the data packs after Attributesetter, so I can't get the item slots here.
                var idSelector = selector == null ? null : selector.find(IdSelector.class);
                ResourceLocation itemId = idSelector == null ? null : idSelector.id;

                if (itemId == null)
                    return null;
                var item = BuiltInRegistries.ITEM.get(itemId);
                if (item == null)
                    return null;
                var itemstack = new ItemStack(item);
                var cSlots = CuriosApi.getItemStackSlots(itemstack, FMLLoader.getDist().isClient());
                if (cSlots.isEmpty())
                    return null;
                slots = new String[cSlots.size()];
                int i = 0;
                for (var entry : cSlots.entrySet())
                {
                    slots[i] = entry.getValue().getIdentifier();
                    i++;
                }
            }
            else if (!slotEl.getAsString().startsWith("curio:"))
                return null;
            else
                slots = new String[] { slotEl.getAsString().substring("curio:".length()) };

            var attr = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(attrEl.getAsString()));
            if (attr == null || attr.isEmpty())
            {
                Attributesetter.LOGGER.warn("Curio item setter {} has an invalid attribute {}", id, attrEl.getAsString());
                return null;
            }
            var valueEl = obj.get("value");
            if (valueEl == null)
            {
                Attributesetter.LOGGER.warn("Curio item setter {} is missing an amount", id);
                return null;
            }
            double value = valueEl.getAsDouble();
            var uniqueIndex = id.toString();
            ASSetter<ItemStack>[] setters = new ASSetter[slots.length];
            int i = 0;
            for (var slot : slots)
            {
                if (opEl != null)
                {
                    var opStr = opEl.getAsString().toUpperCase();
                    if (opStr.equalsIgnoreCase("base"))
                    {
                        setters[i] = new CurioItemBaseSetter(attr.get(), value, slot, uniqueIndex);
                        continue;
                    }

                    AttributeModifier.Operation op;
                    try {
                        op = AttributeModifier.Operation.valueOf(opStr);
                    } catch (IllegalArgumentException ex)
                    {
                        Attributesetter.LOGGER.warn("Curio item setter {} has an invalid operation {}", id, opStr);
                        return null;
                    }
                    setters[i] = new CurioItemModifierSetter(attr.get(), value, op, slot, uniqueIndex);
                }
                else
                {
                    setters[i] = new CurioItemModifierSetter(attr.get(), value, AttributeModifier.Operation.ADD_VALUE, slot, uniqueIndex);
                }
                i++;
            }
            if (setters.length == 1)
                return setters[0];
            else
                return new CompositeASSetter<ItemStack>(setters);
        });
    }
}
