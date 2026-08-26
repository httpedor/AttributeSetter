package com.httpedro.attributesetter.targettypes;

import com.httpedro.attributesetter.targettypes.interfaces.IDataComponentHolderTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IIdentifiableTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IRegistryAssociatedTargetType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ItemTargetType extends SingletonTargetType<Item> implements IIdentifiableTargetType<Item>,
        IDataComponentHolderTargetType<Item>,
        IRegistryAssociatedTargetType<Item> {
    public ItemTargetType() {
        super(Item.class);
    }

    @Override
    public String getFolderName() {
        return "item_type";
    }

    @Override
    public java.util.List<String> getFolderAliases() {
        return java.util.List.of("item_types", "itemtype");
    }

    @Override
    public ResourceLocation getId(Item obj) {
        return BuiltInRegistries.ITEM.getKey(obj);
    }

    @Override
    public DataComponentMap getDataComponentMap(Item obj) {
        return obj.components();
    }

    @Override
    public Registry<Item> getRegistry() {
        return BuiltInRegistries.ITEM;
    }
}
