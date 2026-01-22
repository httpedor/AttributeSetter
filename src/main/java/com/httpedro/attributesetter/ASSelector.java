package com.httpedro.attributesetter;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class ASSelector {
    public SelectorType type;
    public ResourceLocation id;
    public CompoundTag nbt = null;
    public boolean inverted = false;

    public ASSelector(SelectorType type, ResourceLocation id) {
        this.type = type;
        this.id = id;
    }

    public static ASSelector parse(String str, String defaultNamespace)
    {
        if (str.startsWith("!"))
        {
            var selector = parse(str.substring(1), defaultNamespace);
            selector.inverted = true;
            return selector;
        }

        if (str.startsWith("#"))
        {
            ResourceLocation id;
            if (str.contains(":"))
                id = new ResourceLocation(str.substring(1));
            else
                id = new ResourceLocation(defaultNamespace, str.substring(1));
            return new ASSelector(SelectorType.TAG, id);
        }
        else
        {
            if (str.contains("{") && str.contains("}"))
            {
                String nbt = str.substring(str.indexOf("{"), str.lastIndexOf("}") + 1);
                String idStr = str.substring(0, str.indexOf("{"));
                CompoundTag nbtTag;
                try {
                    nbtTag = new TagParser(new StringReader(nbt)).readStruct();
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException("Failed to parse NBT in ASSelector: " + str, e);
                }
                ResourceLocation id;
                if (idStr.isEmpty())
                    id = null;
                else if (idStr.contains(":"))
                    id = new ResourceLocation(idStr);
                else
                    id = new ResourceLocation(defaultNamespace, idStr);
                var selector = new ASSelector(SelectorType.NBT, id);
                selector.nbt = nbtTag;
                return selector;
            }
            else
            {
                ResourceLocation id;
                if (str.contains(":"))
                    id = new ResourceLocation(str);
                else
                    id = new ResourceLocation(defaultNamespace, str);
                return new ASSelector(SelectorType.ID, id);
            }
        }
    }

    public boolean test(Entity obj)
    {
        var ret = false;
        switch (type) {
            case TAG:
                var tag = new TagKey<>(Registries.ENTITY_TYPE, id);
                ret = obj.getType().is(tag);
                break;
            case ID:
                ret = EntityType.getKey(obj.getType()).equals(id);
                break;
            case NBT:
                var nbtTag = obj.saveWithoutId(new CompoundTag());
                if (nbt == null) return false;
                ret = NbtUtils.compareNbt(nbt, nbtTag, true);
                break;
            default:
                return false;
        }
        return inverted ? !ret : ret;
    }
    public boolean test(ItemStack item)
    {
        var ret = false;
        switch (type) {
            case TAG:
                var tag = new TagKey<>(Registries.ITEM, id);
                ret = item.is(tag);
                break;
            case ID:
                ret = ForgeRegistries.ITEMS.getKey(item.getItem()).equals(id);
                break;
            case NBT:
                if (nbt == null) return false;
                CompoundTag itemNbt = item.save(new CompoundTag());
                if (itemNbt == null) return false;
                ret = NbtUtils.compareNbt(nbt, itemNbt, true);
                if (id != null)
                    ret = ret && ForgeRegistries.ITEMS.getKey(item.getItem()).equals(id);
                break;
            default:
                return false;
        }
        return inverted ? !ret : ret;
    }
}