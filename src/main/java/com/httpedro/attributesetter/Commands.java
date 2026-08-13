package com.httpedro.attributesetter;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.httpedro.attributesetter.api.UniqueRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * {@code /attributesetter unique <item|entity|group> <id> [get|set <n>|add <n>|remove <n>|reset]} - reads and edits
 * the per-save unique counters held in {@link UniqueRegistry}. Works on any id, whether or not a cap is currently
 * active for it, so admins can pre-seed or fix up counts.
 */
public class Commands {
    private static final DynamicCommandExceptionType UNKNOWN_ITEM =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown item: " + id));
    private static final DynamicCommandExceptionType UNKNOWN_ENTITY =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown entity type: " + id));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        LiteralArgumentBuilder<CommandSourceStack> unique = literal("unique");

        // item <id>
        unique.then(literal("item").then(countActions(
                argument("item", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), b)),
                Commands::itemKey, Commands::itemName)));

        // entity <id>
        unique.then(literal("entity").then(countActions(
                argument("entity", ResourceLocationArgument.id())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), b)),
                Commands::entityKey, Commands::entityName)));

        // group <id>
        unique.then(literal("group").then(countActions(
                argument("group", StringArgumentType.string())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(UniqueRegistry.getGroupIds(), b)),
                Commands::groupKey, Commands::groupName)));

        dispatcher.register(literal("attributesetter")
                .requires(src -> src.hasPermission(2))
                .then(unique));
    }

    /** Resolves the count key for the id argument on the given context; throws for unknown item/entity ids. */
    @FunctionalInterface
    private interface KeyFn {
        String apply(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    /**
     * Hangs the get/set/add/remove/reset verbs (and a bare "get" default) off an id argument node. The two
     * functions turn the parsed id into a saved-data count key and a human-readable display name.
     */
    private static <T> com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, T> countActions(
            com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, T> arg, KeyFn keyFn, KeyFn nameFn)
    {
        arg.executes(ctx -> report(ctx, keyFn.apply(ctx), nameFn.apply(ctx)));
        arg.then(literal("get").executes(ctx -> report(ctx, keyFn.apply(ctx), nameFn.apply(ctx))));
        arg.then(literal("reset").executes(ctx -> setCount(ctx, keyFn.apply(ctx), nameFn.apply(ctx), 0)));
        arg.then(literal("set").then(argument("value", IntegerArgumentType.integer(0))
                .executes(ctx -> setCount(ctx, keyFn.apply(ctx), nameFn.apply(ctx), IntegerArgumentType.getInteger(ctx, "value")))));
        arg.then(literal("add").then(argument("value", IntegerArgumentType.integer(1))
                .executes(ctx -> addCount(ctx, keyFn.apply(ctx), nameFn.apply(ctx), IntegerArgumentType.getInteger(ctx, "value")))));
        arg.then(literal("remove").then(argument("value", IntegerArgumentType.integer(1))
                .executes(ctx -> addCount(ctx, keyFn.apply(ctx), nameFn.apply(ctx), -IntegerArgumentType.getInteger(ctx, "value")))));
        return arg;
    }

    // ---------------------------------------------------------------------------------------------- key/name fns

    private static Item resolveItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        var id = ResourceLocationArgument.getId(ctx, "item");
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> UNKNOWN_ITEM.create(id));
    }

    private static EntityType<?> resolveEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        var id = ResourceLocationArgument.getId(ctx, "entity");
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow(() -> UNKNOWN_ENTITY.create(id));
    }

    private static String itemKey(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        return UniqueRegistry.itemKey(resolveItem(ctx));
    }

    private static String itemName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        return resolveItem(ctx).getDescription().getString();
    }

    private static String entityKey(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        return UniqueRegistry.entityKey(resolveEntity(ctx));
    }

    private static String entityName(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        return resolveEntity(ctx).getDescription().getString();
    }

    private static String groupKey(CommandContext<CommandSourceStack> ctx)
    {
        return UniqueRegistry.groupKey(StringArgumentType.getString(ctx, "group"));
    }

    private static String groupName(CommandContext<CommandSourceStack> ctx)
    {
        return "group '" + StringArgumentType.getString(ctx, "group") + "'";
    }

    // -------------------------------------------------------------------------------------------------- actions

    private static int report(CommandContext<CommandSourceStack> ctx, String key, String name)
    {
        int count = UniqueRegistry.getCount(key);
        int limit = UniqueRegistry.limitForKey(key);
        String suffix = limit >= 0 ? " / " + limit : "";
        ctx.getSource().sendSuccess(() -> Component.literal(name + " unique count: " + count + suffix), false);
        return count;
    }

    private static int setCount(CommandContext<CommandSourceStack> ctx, String key, String name, int value)
    {
        UniqueRegistry.setCount(key, value);
        int now = UniqueRegistry.getCount(key);
        ctx.getSource().sendSuccess(() -> Component.literal("Set " + name + " unique count to " + now), true);
        return now;
    }

    private static int addCount(CommandContext<CommandSourceStack> ctx, String key, String name, int delta)
    {
        int now = Math.max(0, UniqueRegistry.getCount(key) + delta);
        UniqueRegistry.setCount(key, now);
        ctx.getSource().sendSuccess(() -> Component.literal(name + " unique count is now " + now), true);
        return now;
    }
}
