package com.httpedro.attributesetter;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class TemporaryAttributeInjection {
    public Holder<Attribute> attribute;
    public AttributeModifier modifier;
    public long duration;
    public long endTick;

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("attribute", attribute.unwrapKey().get().location().toString());
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
        return copy;
    }

    public TemporaryAttributeInjection withAmount(double amount)
    {
        TemporaryAttributeInjection copy = copy();
        copy.modifier = new AttributeModifier(modifier.id(), amount, modifier.operation());
        return copy;
    }
    public TemporaryAttributeInjection withAmountMult(double mult)
    {
        TemporaryAttributeInjection copy = copy();
        copy.modifier = new AttributeModifier(modifier.id(), modifier.amount() * mult, modifier.operation());
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
        entry.attribute = BuiltInRegistries.ATTRIBUTE.getHolderOrThrow(ResourceKey.create(Registries.ATTRIBUTE, ResourceLocation.parse(tag.getString("attribute"))));
        entry.modifier = AttributeModifier.load(tag.getCompound("modifier"));
        entry.endTick = tag.getLong("end");
        entry.duration = tag.getLong("duration");
        return entry;
    }
}
