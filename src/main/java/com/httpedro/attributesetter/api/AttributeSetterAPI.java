package com.httpedro.attributesetter.api;

import com.httpedro.attributesetter.util.RegistryValues;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeSetterAPI {
    private static final Map<String, TargetType<?, ?>> ALL = new ConcurrentHashMap<>();
    /** Folder name (and every alias of it) -> the target type a datapack folder of that name means. */
    private static final Map<String, TargetType<?, ?>> BY_FOLDER = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(AttributeSetterAPI.class);

    public static Collection<TargetType<?, ?>> getAllTargetTypes() {
        return ALL.values();
    }
    public static <T, U> TargetType<T, U> registerTargetType(TargetType<T, U> targetType)
    {
        ALL.put(targetType.getFolderName(), targetType);
        BY_FOLDER.put(folderKey(targetType.getFolderName()), targetType);
        for (var alias : targetType.getFolderAliases())
            BY_FOLDER.put(folderKey(alias), targetType);
        return targetType;
    }

    public static TargetType<?, ?> getTargetType(String name)
    {
        var byFolder = BY_FOLDER.get(folderKey(name));
        return byFolder != null ? byFolder : ALL.get(name);
    }

    /**
     * The target types a datapack folder can feed, most specific first: the one the folder names, then whatever
     * it derives from. An entry's setters are offered to each in turn, so a single {@code item} folder takes
     * both per-stack operations (attribute modifiers, tooltips) and per-item ones (durability, food).
     */
    public static List<TargetType<?, ?>> getTargetChain(String name)
    {
        var head = getTargetType(name);
        List<TargetType<?, ?>> chain = new ArrayList<>();
        var current = head;
        // Guarded by size rather than a visited set: a derivation cycle would be a programming error, and this
        // keeps a mistake in a mod's registration from hanging the reload.
        while (current != null && chain.size() < 16)
        {
            chain.add(current);
            current = current.getParent();
        }
        return chain;
    }

    private static String folderKey(String name)
    {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    public static void clearAll()
    {
        for (var targetType : ALL.values())
        {
            targetType.clearEntries();
        }
        AttributeInjector.clearAll();
        RemovalRegistry.clear();
        UniqueRegistry.clearRules();
        BlockReplacementRegistry.clear();
        BlockDefaults.restoreModified();

        for (var item : RegistryValues.of(BuiltInRegistries.ITEM))
        {
            var defaults = TrueDefaults.get(item);
            var builder = DataComponentPatch.builder();
            for (var component : defaults)
                builder.set(component);
            // Components a setter added that the item never had of its own (attribute modifiers on an item with
            // none, food on a non-food, ...) aren't covered by the loop above, so they have to be dropped
            // explicitly or they'd survive the reload that removed the entry.
            for (var component : item.components())
            {
                if (!defaults.has(component.type()))
                    builder.remove(component.type());
            }
            item.modifyDefaultComponentsFrom(builder.build());
        }
    }
}
