package com.httpedro.attributesetter.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * A setter written as a single string instead of an object. The grammar is deliberately tiny:
 *
 * <pre>
 *   &lt;operation&gt; [token]...
 * </pre>
 *
 * where each token is one of
 * <ul>
 *   <li>{@code key=value} - copied straight into the resulting object as that key,</li>
 *   <li>{@code x2} / {@code *2} - a {@code multiplier},</li>
 *   <li>{@code +5} / {@code -5} - an {@code offset},</li>
 *   <li>anything else - a positional argument, whose meaning is up to the operation.</li>
 * </ul>
 *
 * Double quotes group a token that contains spaces. Operations that want the whole tail as one value
 * (tooltips) read {@link #rest} instead of the tokens.
 */
public final class Shorthand {
    private static final String MULT = "[xX*]";
    private static final String NUM = "(\\d+\\.?\\d*|\\.\\d+)";

    /** The original string, trimmed. */
    public final String raw;
    /** The first token, lowercased - what the operation is called. */
    public final String op;
    /** Everything after the operation token, unparsed and unquoted-as-written. */
    public final String rest;
    /** Bare tokens that are neither {@code key=value} nor a multiplier/offset, in order. */
    public final List<String> args;
    /** {@code key=value} tokens. */
    public final Map<String, String> named;
    /** The {@code x2} token, if there was one. */
    public final Float multiplier;
    /** The {@code +5} / {@code -5} token, if there was one. */
    public final Float offset;

    private Shorthand(String raw, String op, String rest, List<String> args, Map<String, String> named,
                      Float multiplier, Float offset)
    {
        this.raw = raw;
        this.op = op;
        this.rest = rest;
        this.args = args;
        this.named = named;
        this.multiplier = multiplier;
        this.offset = offset;
    }

    public static Shorthand parse(String input)
    {
        if (input == null)
            return null;
        var trimmed = input.trim();
        if (trimmed.isEmpty())
            return null;

        var tokens = tokenize(trimmed);
        if (tokens.isEmpty())
            return null;

        var op = tokens.get(0).toLowerCase(Locale.ROOT);
        // The tail is taken from the raw string so operations like `tooltip` keep the spacing the author wrote.
        var rest = trimmed.substring(Math.min(trimmed.length(), firstTokenLength(trimmed))).trim();
        rest = unquote(rest);

        List<String> args = new ArrayList<>();
        Map<String, String> named = new LinkedHashMap<>();
        Float multiplier = null;
        Float offset = null;

        for (int i = 1; i < tokens.size(); i++)
        {
            var token = tokens.get(i);
            int eq = token.indexOf('=');
            if (eq > 0)
            {
                named.put(token.substring(0, eq).trim().toLowerCase(Locale.ROOT), unquote(token.substring(eq + 1).trim()));
                continue;
            }
            if (multiplier == null && token.matches(MULT + NUM))
            {
                multiplier = Float.parseFloat(token.substring(1));
                continue;
            }
            if (offset == null && token.matches("[+-]" + NUM))
            {
                offset = Float.parseFloat(token);
                continue;
            }
            args.add(unquote(token));
        }

        return new Shorthand(trimmed, op, rest, args, named, multiplier, offset);
    }

    /** @return positional argument {@code index}, or {@code null} when the author didn't write one. */
    public String arg(int index)
    {
        return index >= 0 && index < args.size() ? args.get(index) : null;
    }

    public boolean opIs(String... aliases)
    {
        for (var alias : aliases)
        {
            if (op.equals(alias.toLowerCase(Locale.ROOT)))
                return true;
        }
        return false;
    }

    /**
     * The object every shorthand starts from: the operation plus whatever was unambiguous
     * ({@code key=value}, multiplier, offset). Operations layer their own positional handling on top.
     */
    public JsonObject base()
    {
        var obj = new JsonObject();
        obj.addProperty("operation", op);
        for (var entry : named.entrySet())
            obj.add(entry.getKey(), JsonHelper.typed(entry.getValue()));
        if (multiplier != null)
            obj.addProperty("multiplier", multiplier);
        if (offset != null)
            obj.addProperty("offset", offset);
        return obj;
    }

    /**
     * The fallback expansion used when no operation claimed the string: the first positional argument becomes
     * {@code value}, and all of them are also exposed as {@code args} for builders that want more than one.
     */
    public JsonObject toJson()
    {
        var obj = base();
        if (!args.isEmpty())
        {
            obj.add("value", JsonHelper.typed(args.get(0)));
            var arr = new JsonArray();
            for (var arg : args)
                arr.add(JsonHelper.typed(arg));
            obj.add("args", arr);
        }
        return obj;
    }

    private static int firstTokenLength(String input)
    {
        int i = 0;
        while (i < input.length() && !Character.isWhitespace(input.charAt(i)))
            i++;
        return i;
    }

    private static String unquote(String value)
    {
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
            return value.substring(1, value.length() - 1);
        return value;
    }

    private static List<String> tokenize(String input)
    {
        List<String> tokens = new ArrayList<>();
        var current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if (c == '"')
            {
                quoted = !quoted;
                current.append(c);
                continue;
            }
            if (!quoted && Character.isWhitespace(c))
            {
                if (current.length() > 0)
                {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (current.length() > 0)
            tokens.add(current.toString());
        return tokens;
    }
}
