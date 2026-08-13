package com.httpedro.attributesetter;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side config for the {@code make_unique} setters. Only the broadcast defaults live here; the caps
 * themselves come from datapacks.
 */
public class Config {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.ConfigValue<String> DEFAULT_BROADCAST_MESSAGE;
    private static final ModConfigSpec.BooleanValue BROADCAST_BY_DEFAULT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Settings for the make_unique / make_unique_shared setters").push("unique");

        DEFAULT_BROADCAST_MESSAGE = builder
                .comment(
                        "Chat message broadcast when a unique item/entity reaches its cap and the entry doesn't set its own 'message'.",
                        "Placeholders: %name% (item/entity name), %limit% (the cap), %count% (same as %limit% when reached).")
                .define("defaultBroadcastMessage", "The last %name% has appeared! (%limit% now exist)");

        BROADCAST_BY_DEFAULT = builder
                .comment("Whether make_unique entries broadcast to chat when an entry doesn't set its own 'broadcast' flag.")
                .define("broadcastByDefault", true);

        builder.pop();

        SPEC = builder.build();
    }

    public static String defaultBroadcastMessage()
    {
        return DEFAULT_BROADCAST_MESSAGE.get();
    }

    public static boolean broadcastByDefault()
    {
        return BROADCAST_BY_DEFAULT.get();
    }
}
