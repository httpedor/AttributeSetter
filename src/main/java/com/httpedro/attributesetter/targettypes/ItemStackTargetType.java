package com.httpedro.attributesetter.targettypes;

import java.util.function.Function;

import com.httpedro.attributesetter.Attributesetter;

import com.httpedro.attributesetter.targettypes.interfaces.IDataComponentHolderTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.INBTSerializableTargetType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemStackTargetType extends RegistryTargetType<ItemStack, Item> implements INBTSerializableTargetType<ItemStack>,
		IDataComponentHolderTargetType<ItemStack> {

	public ItemStackTargetType() {
		super(ItemStack.class, BuiltInRegistries.ITEM);
	}

	@Override
    public String getFolderName() {
        return "item";
    }

	@Override
	public java.util.List<String> getFolderAliases() {
		return java.util.List.of("items", "itemstack", "item_stack", "itemstacks");
	}

	@Override
	public Item getSingleton(ItemStack instance) {
	    return instance.getItem();
	}

	@Override
	public Function<ItemStack, Tag> getSerializer() {
	    return (stack) -> stack.save(Attributesetter.ra, new CompoundTag());
	}

	@Override
	public DataComponentMap getDataComponentMap(ItemStack obj) {
		return obj.getComponents();
	}
}
