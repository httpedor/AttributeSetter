package com.httpedro.attributesetter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.core.component.DataComponents;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AttributeSetterClient {

    private static final String MAINHAND_SLOT = "mainhand";
    private static final String DMG_ATTR_NAME = "attribute.name.generic.attack_damage";
    private static final String SPD_ATTR_NAME = "attribute.name.generic.attack_speed";

    public static void mergeTooltips(ItemTooltipEvent e)
    {
        if (Attributesetter.isApothic)
            return;
        var original = new ArrayList<>(e.getToolTip());
        try
        {
            var lines = e.getToolTip();
            Map<String, Map<String, Double>> blueAttributes = new HashMap<>();
            Map<String, Integer> slotIndexes = new HashMap<>();
            Map<String, Double> greenAttributes = new HashMap<>();
            Map<String, MainhandTotals> mainhandTotals = new HashMap<>();
            String currentSlot = null;
            int i = 0;
            for (Iterator<Component> it = lines.iterator(); it.hasNext();)
            {
                var line = it.next();
                var content = line.getContents();
                //Normal attr modifiers
                if (content instanceof TranslatableContents ttc)
                {
                    if (ttc.getKey().startsWith("item.modifiers"))
                    {
                        currentSlot = ttc.getKey().substring(ttc.getKey().lastIndexOf('.')+1);
                        slotIndexes.put(currentSlot, i);
                    }
                    else if (ttc.getKey().startsWith("attribute.modifier.plus.") && currentSlot != null)
                    {
                        var value = parseAttributeAmount(ttc.getArgs()[0]);
                        if (value == null)
                        {
                            i++;
                            continue;
                        }
                        var attrName = ((TranslatableContents)((MutableComponent)ttc.getArgs()[1]).getContents()).getKey();
                        int op = parseModifierOperationSuffix(ttc.getKey());
                        if (isMainhandDamageOrSpeed(attrName, currentSlot) && op >= 0)
                        {
                            var totals = mainhandTotals.computeIfAbsent(attrName, key -> new MainhandTotals());
                            totals.sawModifier = true;
                            totals.addValue += op == 0 ? value : 0.0;
                            totals.multBase += op == 1 ? value : 0.0;
                            totals.multTotal += op == 2 ? value : 0.0;
                            it.remove();
                        }
                        else if (greenAttributes.containsKey(attrName))
                        {
                            greenAttributes.put(attrName, greenAttributes.get(attrName) + value);
                            it.remove();
                        }
                        else
                        {
                            if (!blueAttributes.containsKey(currentSlot))
                                blueAttributes.put(currentSlot, new HashMap<>());

                            blueAttributes.get(currentSlot).put(attrName, blueAttributes.get(currentSlot).getOrDefault(attrName, 0.0) + value);
                            it.remove();
                        }
                    }
                    else if (ttc.getKey().startsWith("attribute.modifier.take.") && currentSlot != null)
                    {
                        var value = parseAttributeAmount(ttc.getArgs()[0]);
                        if (value == null)
                        {
                            i++;
                            continue;
                        }
                        var attrName = ((TranslatableContents)((MutableComponent)ttc.getArgs()[1]).getContents()).getKey();
                        int op = parseModifierOperationSuffix(ttc.getKey());
                        if (isMainhandDamageOrSpeed(attrName, currentSlot) && op >= 0)
                        {
                            var totals = mainhandTotals.computeIfAbsent(attrName, key -> new MainhandTotals());
                            totals.sawModifier = true;
                            totals.addValue += op == 0 ? -value : 0.0;
                            totals.multBase += op == 1 ? -value : 0.0;
                            totals.multTotal += op == 2 ? -value : 0.0;
                            it.remove();
                        }
                        else if (greenAttributes.containsKey(attrName))
                        {
                            greenAttributes.put(attrName, greenAttributes.get(attrName) - value);
                            it.remove();
                        }
                        else
                        {
                            if (!blueAttributes.containsKey(currentSlot))
                                blueAttributes.put(currentSlot, new HashMap<>());
                            blueAttributes.get(currentSlot).put(attrName, blueAttributes.get(currentSlot).getOrDefault(attrName, 0.0) - value);
                            it.remove();
                        }
                    }
                }
                else
                {
                    for (var part : line.getSiblings())
                    {
                        if (part.getContents() instanceof TranslatableContents ttc && ttc.getKey().startsWith("attribute.modifier.equals.0"))
                        {
                            var value = parseAttributeAmount(ttc.getArgs()[0]);
                            if (value == null)
                            {
                                i++;
                                continue;
                            }
                            var attrName = ((TranslatableContents)((MutableComponent)ttc.getArgs()[1]).getContents()).getKey();
                            if (isMainhandDamageOrSpeed(attrName, currentSlot))
                            {
                                var totals = mainhandTotals.computeIfAbsent(attrName, key -> new MainhandTotals());
                                totals.equalsValue = value;
                                it.remove();
                            }
                            else
                            {
                                greenAttributes.put(attrName, value);
                                it.remove();
                            }
                        }
                    }
                }
                i++;
            }
            for (var entry : mainhandTotals.entrySet())
            {
                var attrName = entry.getKey();
                var totals = entry.getValue();
                if (!totals.sawModifier && totals.equalsValue == null)
                    continue;
                double base = attrName.equals(DMG_ATTR_NAME) ? 1.0 : 4.0;
                double total = totals.sawModifier
                        ? (base + totals.addValue) * (1.0 + totals.multBase) * (1.0 + totals.multTotal)
                        : totals.equalsValue;
                greenAttributes.put(attrName, total);
            }
            for (var slotEntry : blueAttributes.entrySet())
            {
                i = 0;
                var slot = slotEntry.getKey();
                for (var entry : slotEntry.getValue().entrySet())
                {
                    var attrName = entry.getKey();
                    var value = entry.getValue();
                    if (value == 0)
                    {
                        i++;
                        continue;
                    }
                    var color = value > 0 ? ChatFormatting.BLUE : ChatFormatting.RED;
                    var line = Component.translatable(value > 0 ? "attribute.modifier.plus.0" : "attribute.modifier.take.0", Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(value))).withStyle(color), Component.translatable(attrName).withStyle(color)).withStyle(color);
                    lines.add(slotIndexes.get(slot) + i + 1, line);
                    i++;
                }
            }
            i = 0;
            for (var entry : greenAttributes.entrySet())
            {
                var attrName = entry.getKey();
                var value = entry.getValue();
                var color = ChatFormatting.DARK_GREEN;
                var line = Component.literal(" ").append(Component.translatable("attribute.modifier.equals.0", Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value)).withStyle(color), Component.translatable(attrName).withStyle(color)));
                lines.add(slotIndexes.get(MAINHAND_SLOT) + i + 1, line);
                i++;
            }
        } catch (Exception ex)
        {
            e.getToolTip().clear();
            e.getToolTip().addAll(original);
        }

    }

    private static boolean isMainhandDamageOrSpeed(String attrName, String currentSlot)
    {
        return MAINHAND_SLOT.equals(currentSlot) && (DMG_ATTR_NAME.equals(attrName) || SPD_ATTR_NAME.equals(attrName));
    }

    private static int parseModifierOperationSuffix(String key)
    {
        int lastDot = key.lastIndexOf('.');
        if (lastDot == -1 || lastDot + 1 >= key.length())
            return -1;
        try
        {
            return Integer.parseInt(key.substring(lastDot + 1));
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    private static Double parseAttributeAmount(Object arg)
    {
        if (!(arg instanceof Component component))
            return null;
        var text = component.getString();
        if (!NumberUtils.isCreatable(text))
            return null;
        return Double.parseDouble(text);
    }

    private static final class MainhandTotals
    {
        double addValue;
        double multBase;
        double multTotal;
        Double equalsValue;
        boolean sawModifier;
    }

}
