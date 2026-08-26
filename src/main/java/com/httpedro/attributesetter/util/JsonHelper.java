package com.httpedro.attributesetter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;

/**
 * Small reading helpers shared by every selector/setter builder. They all follow the same convention: the first
 * key that is actually present wins, so a builder can accept several spellings of the same field
 * ({@code with} / {@code to} / {@code value}) without repeating the null dance each time.
 */
public final class JsonHelper {
    private JsonHelper() {}

    /** @return the value of the first of {@code keys} present (and non-null) on {@code obj}, or {@code null}. */
    public static JsonElement first(JsonObject obj, String... keys)
    {
        if (obj == null)
            return null;
        for (var key : keys)
        {
            var el = obj.get(key);
            if (el != null && !el.isJsonNull())
                return el;
        }
        return null;
    }

    public static boolean hasAny(JsonObject obj, String... keys)
    {
        return first(obj, keys) != null;
    }

    public static String string(JsonObject obj, String def, String... keys)
    {
        var el = first(obj, keys);
        return el == null || !el.isJsonPrimitive() ? def : el.getAsString();
    }

    public static float number(JsonObject obj, float def, String... keys)
    {
        var el = first(obj, keys);
        return el == null || !el.isJsonPrimitive() ? def : el.getAsFloat();
    }

    public static int integer(JsonObject obj, int def, String... keys)
    {
        var el = first(obj, keys);
        return el == null || !el.isJsonPrimitive() ? def : el.getAsInt();
    }

    public static boolean bool(JsonObject obj, boolean def, String... keys)
    {
        var el = first(obj, keys);
        return el == null || !el.isJsonPrimitive() ? def : el.getAsBoolean();
    }

    /** Treats a lone value as a one-element list, so every "may be a list" field can be written either way. */
    public static List<JsonElement> list(JsonElement el)
    {
        List<JsonElement> out = new ArrayList<>();
        if (el == null || el.isJsonNull())
            return out;
        if (el.isJsonArray())
        {
            for (var item : el.getAsJsonArray())
            {
                if (item != null && !item.isJsonNull())
                    out.add(item);
            }
        }
        else
            out.add(el);
        return out;
    }

    public static List<String> strings(JsonElement el)
    {
        List<String> out = new ArrayList<>();
        for (var item : list(el))
        {
            if (item.isJsonPrimitive())
                out.add(item.getAsString());
        }
        return out;
    }

    /**
     * Reads an NBT value written either as SNBT ({@code "{Damage:0}"}) or as a plain JSON object
     * ({@code {"Damage": 0}}), which is what most people reach for first inside a JSON file.
     */
    public static CompoundTag compound(JsonElement el) throws Exception
    {
        if (el == null || el.isJsonNull())
            return null;
        if (el.isJsonPrimitive())
            return new TagParser(new StringReader(el.getAsString())).readStruct();
        Tag tag = new Dynamic<>(JsonOps.INSTANCE, el).convert(NbtOps.INSTANCE).getValue();
        if (tag instanceof CompoundTag compound)
            return compound;
        throw new IllegalArgumentException("NBT selector needs an object or an SNBT string, got " + el);
    }

    /** Turns a shorthand token into the JSON value it looks like: {@code 12} / {@code true} / {@code "sword"}. */
    public static JsonElement typed(String raw)
    {
        if (raw == null)
            return null;
        var trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false"))
            return new JsonPrimitive(Boolean.parseBoolean(trimmed));
        if (trimmed.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)"))
        {
            try {
                if (trimmed.contains("."))
                    return new JsonPrimitive(Double.parseDouble(trimmed));
                return new JsonPrimitive(Long.parseLong(trimmed));
            } catch (NumberFormatException ignored) {}
        }
        return new JsonPrimitive(trimmed);
    }

    public static JsonArray arrayOf(List<String> values)
    {
        var arr = new JsonArray();
        for (var v : values)
            arr.add(v);
        return arr;
    }

    public static String key(String raw)
    {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }
}
