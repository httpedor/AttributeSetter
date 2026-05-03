package com.httpedor.attributesetter.mixin;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.httpedor.attributesetter.compat.TrinketsCompat;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Pseudo
@Mixin(Trinket.class)
public class TrinketsMixin {

    /**
     * @author httpedor
     * @reason Can't inject into interface default methods. This is kinda dangerous but as I'm making a branch for each MC version, it should be fine
     */
    @Overwrite
    public Multimap<EntityAttribute, EntityAttributeModifier> getModifiers(ItemStack stack,
                                                                          SlotReference slot, LivingEntity entity, UUID uuid)
    {
        Multimap<EntityAttribute, EntityAttributeModifier> map = Multimaps.newMultimap(Maps.newLinkedHashMap(), ArrayList::new);

        if (stack.hasNbt() && stack.getNbt().contains("TrinketAttributeModifiers", 9)) {
            NbtList list = stack.getNbt().getList("TrinketAttributeModifiers", 10);

            for (int i = 0; i < list.size(); i++) {
                NbtCompound tag = list.getCompound(i);

                if (!tag.contains("Slot", NbtType.STRING) || tag.getString("Slot")
                        .equals(slot.inventory().getSlotType().getGroup() + "/" + slot.inventory().getSlotType().getName())) {
                    Optional<EntityAttribute> optional = Registries.ATTRIBUTE
                            .getOrEmpty(Identifier.tryParse(tag.getString("AttributeName")));

                    if (optional.isPresent()) {
                        EntityAttributeModifier entityAttributeModifier = EntityAttributeModifier.fromNbt(tag);

                        if (entityAttributeModifier != null
                                && entityAttributeModifier.getId().getLeastSignificantBits() != 0L
                                && entityAttributeModifier.getId().getMostSignificantBits() != 0L) {
                            map.put(optional.get(), entityAttributeModifier);
                        }
                    }
                }
            }
        }

        // MY CODE
        var id = Registries.ITEM.getId(stack.getItem());
        var slotName = slot.inventory().getSlotType().getName();
        for (var entry : TrinketsCompat.BASE_TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.isIn(TagKey.of(RegistryKeys.ITEM, entry.getKey()))
                    && entry.getValue().containsKey(slotName))
            {
                for (var modEntry : entry.getValue().get(slotName).entrySet())
                {
                    map.removeAll(modEntry.getKey());
                    Pair<Double, UUID> val = modEntry.getValue();
                    map.put(modEntry.getKey(), new EntityAttributeModifier(val.getB(), "ASMod", val.getA(), EntityAttributeModifier.Operation.ADDITION));
                }
            }
        }
        for (var entry : TrinketsCompat.BASE_ITEM_MODIFIERS.entrySet())
        {
            if (entry.getKey().equals(id) && entry.getValue().containsKey(slotName))
            {
                for (var modEntry : entry.getValue().get(slotName).entrySet())
                {
                    map.removeAll(modEntry.getKey());
                    Pair<Double, UUID> val = modEntry.getValue();
                    map.put(modEntry.getKey(), new EntityAttributeModifier(val.getB(), "ASMod", val.getA(), EntityAttributeModifier.Operation.ADDITION));
                }
            }
        }
        for (var entry : TrinketsCompat.TAG_ITEM_MODIFIERS.entrySet())
        {
            if (stack.isIn(TagKey.of(RegistryKeys.ITEM, entry.getKey())) && entry.getValue().containsKey(slotName))
            {
                for (var modEntry : entry.getValue().get(slotName).entrySet())
                {
                    var val = modEntry.getValue();
                    var clone = new EntityAttributeModifier(uuid, val.getName(), val.getValue(), val.getOperation());
                    map.put(modEntry.getKey(), clone);
                }
            }
        }

        var modifiers = TrinketsCompat.ITEM_MODIFIERS.getOrDefault(id, null);
        if (modifiers != null)
        {
            var slotMods = modifiers.getOrDefault(slotName, null);
            if (slotMods != null)
            {
                for (var entry : slotMods.entrySet())
                {
                    var val = entry.getValue();
                    var clone = new EntityAttributeModifier(uuid, val.getName(), val.getValue(), val.getOperation());
                    map.put(entry.getKey(), clone);
                }
            }
        }
        return map;
    }
}
