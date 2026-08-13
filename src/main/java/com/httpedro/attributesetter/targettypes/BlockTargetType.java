package com.httpedro.attributesetter.targettypes;

import com.httpedro.attributesetter.targettypes.interfaces.IIdentifiableTargetType;
import com.httpedro.attributesetter.targettypes.interfaces.IRegistryAssociatedTargetType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class BlockTargetType extends SingletonTargetType<Block> implements IIdentifiableTargetType<Block>,
        IRegistryAssociatedTargetType<Block> {
    public BlockTargetType() {
        super(Block.class);
    }

    @Override
    public String getFolderName() {
        return "block";
    }

    @Override
    public ResourceLocation getId(Block obj) {
        return BuiltInRegistries.BLOCK.getKey(obj);
    }

    @Override
    public Registry<Block> getRegistry() {
        return BuiltInRegistries.BLOCK;
    }
}
