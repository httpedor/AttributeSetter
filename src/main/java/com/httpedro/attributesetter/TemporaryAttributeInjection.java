package com.httpedro.attributesetter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

public class TemporaryAttributeInjection {
    public Attribute attribute;
    public AttributeModifier modifier;
    public long duration;
    public long endTick;

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("attribute", ForgeRegistries.ATTRIBUTES.getKey(attribute).toString());
        tag.put("modifier", modifier.save());
        tag.putLong("end", endTick);
        tag.putLong("duration", duration);
        return tag;
    }

    public TemporaryAttributeInjection copy()
    {
        TemporaryAttributeInjection copy = new TemporaryAttributeInjection();
        copy.attribute = this.attribute;
        copy.modifier = this.modifier;
        copy.endTick = this.endTick;
        copy.duration = this.duration;
        return copy;
    }

    public TemporaryAttributeInjection withAmount(double amount)
    {
        TemporaryAttributeInjection copy = copy();
        copy.modifier = new AttributeModifier(modifier.getId(), modifier.getName(), amount, modifier.getOperation());
        return copy;
    }
    public TemporaryAttributeInjection withAmountMult(double mult)
    {
        TemporaryAttributeInjection copy = copy();
        copy.modifier = new AttributeModifier(modifier.getId(), modifier.getName(), modifier.getAmount() * mult, modifier.getOperation());
        return copy;
    }
    public TemporaryAttributeInjection withDuration(long duration)
    {
        TemporaryAttributeInjection copy = copy();
        copy.duration = duration;
        return copy;
    }
    public TemporaryAttributeInjection withDurationMult(double mult)
    {
        TemporaryAttributeInjection copy = copy();
        copy.duration = (long) (copy.duration * mult);
        return copy;
    }

    public static TemporaryAttributeInjection fromTag(CompoundTag tag) {
        TemporaryAttributeInjection entry = new TemporaryAttributeInjection();
        entry.attribute = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(tag.getString("attribute")));
        entry.modifier = AttributeModifier.load(tag.getCompound("modifier"));
        entry.endTick = tag.getLong("end");
        entry.duration = tag.getLong("duration");
        return entry;
    }
}
