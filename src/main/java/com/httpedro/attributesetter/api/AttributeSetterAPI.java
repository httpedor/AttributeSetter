package com.httpedro.attributesetter.api;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeSetterAPI {
    private static final Map<String, TargetType<?, ?>> ALL = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(AttributeSetterAPI.class);

    public static Collection<TargetType<?, ?>> getAllTargetTypes() {
        return ALL.values();
    }
    public static <T, U> TargetType<T, U> registerTargetType(TargetType<T, U> targetType)
    {
        ALL.put(targetType.getFolderName(), targetType);
        return targetType;
    }

    public static TargetType<?, ?> getTargetType(String name)
    {
        return ALL.get(name);
    }

    public static void clearAll()
    {
        for (var targetType : ALL.values())
        {
            targetType.clearEntries();
        }
        AttributeInjector.clearAll();

        for (var item : BuiltInRegistries.ITEM)
        {
            var defaults = TrueDefaults.get(item);
            var builder = DataComponentPatch.builder();
            for (var component : defaults)
                builder.set(component);
            item.modifyDefaultComponentsFrom(builder.build());
        }
    }
}
