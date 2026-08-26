package com.httpedro.attributesetter;

import com.httpedro.attributesetter.api.AttributeSetterAPI;
import com.httpedro.attributesetter.api.TargetType;
import com.httpedro.attributesetter.targettypes.AttributeTargetType;
import com.httpedro.attributesetter.targettypes.BlockTargetType;
import com.httpedro.attributesetter.targettypes.EntityTargetType;
import com.httpedro.attributesetter.targettypes.ItemStackTargetType;

import com.httpedro.attributesetter.targettypes.ItemTargetType;
import com.httpedro.attributesetter.targettypes.RecipeTargetType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class TargetTypes {
    public static final TargetType<LivingEntity, EntityType<?>> ENTITY = AttributeSetterAPI.registerTargetType(new EntityTargetType());
    public static final TargetType<Item, Item> ITEM = AttributeSetterAPI.registerTargetType(new ItemTargetType());
    public static final TargetType<ItemStack, Item> ITEMSTACK = AttributeSetterAPI.registerTargetType(new ItemStackTargetType());
    public static final TargetType<Attribute, Attribute> ATTRIBUTE = AttributeSetterAPI.registerTargetType(new AttributeTargetType());
    public static final TargetType<Block, Block> BLOCK = AttributeSetterAPI.registerTargetType(new BlockTargetType());
    public static final TargetType<RecipeHolder<?>, ResourceLocation> RECIPE = AttributeSetterAPI.registerTargetType(new RecipeTargetType());

    static {
        // A stack is a specific instance of an item, so everything that can be said about an item can be said
        // about a stack: an item selector (id, tag, regex, "isFood", ...) works on stacks by looking at the
        // stack's item, and the `item` folder accepts item-level setters by handing them to ITEM.
        ITEMSTACK.derivesFrom(ITEM, ItemStack::getItem);
    }

    public static void bootstrap(){}
}
