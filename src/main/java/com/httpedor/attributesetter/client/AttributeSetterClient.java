package com.httpedor.attributesetter.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.httpedor.attributesetter.AttributeSetter;
import com.httpedor.attributesetter.AttributeSetterAPI;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AttributeSetterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, ctx, lines) -> {
            var original = new ArrayList<>(lines);
            try
            {
                Map<String, Map<String, Double>> blueAttributes = new HashMap<>();
                Map<String, Integer> slotIndexes = new HashMap<>();
                Map<String, Double> greenAttributes = new HashMap<>();
                String currentSlot = null;
                int i = 0;
                for (Iterator<Text> it = lines.iterator(); it.hasNext();)
                {
                    var line = it.next();
                    var content = line.getContent();
                    if (content instanceof TranslatableTextContent ttc)
                    {
                        String dmgAttrName = "attribute.name.generic.attack_damage";
                        String spdAttrName = "attribute.name.generic.attack_speed";
                        if (ttc.getKey().startsWith("item.modifiers"))
                        {
                            currentSlot = ttc.getKey().substring(ttc.getKey().lastIndexOf('.') + 1);
                            slotIndexes.put(currentSlot, i);
                        }
                        else if (ttc.getKey().startsWith("attribute.modifier.plus.0") && currentSlot != null)
                        {
                            if (!NumberUtils.isCreatable(ttc.getArg(0).getString()))
                            {
                                i++;
                                continue;
                            }
                            if (!(ttc.getArg(1) instanceof MutableText mutableText) || !(mutableText.getContent() instanceof TranslatableTextContent attrContent))
                            {
                                i++;
                                continue;
                            }

                            var attrName = attrContent.getKey();
                            double value = Double.parseDouble(ttc.getArg(0).getString());
                            if (greenAttributes.containsKey(attrName))
                            {
                                greenAttributes.put(attrName, greenAttributes.get(attrName) + value);
                                it.remove();
                            }
                            else if (attrName.equals(dmgAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, value + 1);
                                it.remove();
                            }
                            else if (attrName.equals(spdAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, 4 + value);
                                it.remove();
                            }
                            else
                            {
                                blueAttributes.putIfAbsent(currentSlot, new HashMap<>());
                                var currentValue = blueAttributes.get(currentSlot).getOrDefault(attrName, 0.0);
                                blueAttributes.get(currentSlot).put(attrName, currentValue + value);
                                it.remove();
                            }
                        }
                        else if (ttc.getKey().startsWith("attribute.modifier.take.0") && currentSlot != null)
                        {
                            if (!NumberUtils.isCreatable(ttc.getArg(0).getString()))
                            {
                                i++;
                                continue;
                            }
                            if (!(ttc.getArg(1) instanceof MutableText mutableText) || !(mutableText.getContent() instanceof TranslatableTextContent attrContent))
                            {
                                i++;
                                continue;
                            }

                            var attrName = attrContent.getKey();
                            double value = Double.parseDouble(ttc.getArg(0).getString());
                            if (greenAttributes.containsKey(attrName))
                            {
                                greenAttributes.put(attrName, greenAttributes.get(attrName) - value);
                                it.remove();
                            }
                            else if (attrName.equals(dmgAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, -value + 1);
                                it.remove();
                            }
                            else if (attrName.equals(spdAttrName) && currentSlot.equals("mainhand"))
                            {
                                greenAttributes.put(attrName, 4 - value);
                                it.remove();
                            }
                            else
                            {
                                blueAttributes.putIfAbsent(currentSlot, new HashMap<>());
                                var currentValue = blueAttributes.get(currentSlot).getOrDefault(attrName, 0.0);
                                blueAttributes.get(currentSlot).put(attrName, currentValue - value);
                                it.remove();
                            }
                        }
                    }
                    else
                    {
                        boolean removeLine = false;
                        for (var part : line.getSiblings())
                        {
                            if (part.getContent() instanceof TranslatableTextContent ttc && ttc.getKey().startsWith("attribute.modifier.equals.0"))
                            {
                                if (!NumberUtils.isCreatable(ttc.getArg(0).getString()))
                                {
                                    i++;
                                    continue;
                                }
                                if (!(ttc.getArg(1) instanceof MutableText mutableText) || !(mutableText.getContent() instanceof TranslatableTextContent attrContent))
                                {
                                    i++;
                                    continue;
                                }

                                var attrName = attrContent.getKey();
                                greenAttributes.put(attrName, Double.parseDouble(ttc.getArg(0).getString()));
                                removeLine = true;
                                break;
                            }
                        }
                        if (removeLine)
                            it.remove();
                    }
                    i++;
                }

                for (var slotEntry : blueAttributes.entrySet())
                {
                    i = 0;
                    var slot = slotEntry.getKey();
                    var slotIndex = slotIndexes.get(slot);
                    if (slotIndex == null)
                        continue;

                    for (var entry : slotEntry.getValue().entrySet())
                    {
                        var attrName = entry.getKey();
                        var value = entry.getValue();
                        if (value == 0)
                        {
                            i++;
                            continue;
                        }
                        var color = value > 0 ? Formatting.BLUE : Formatting.RED;
                        var line = Text.translatable(
                            value > 0 ? "attribute.modifier.plus.0" : "attribute.modifier.take.0",
                            Text.literal(ItemStack.MODIFIER_FORMAT.format(Math.abs(value))).formatted(color),
                            Text.translatable(attrName).formatted(color)
                        ).formatted(color);
                        lines.add(slotIndex + i + 1, line);
                        i++;
                    }
                }

                i = 0;
                var mainhandSlotIndex = slotIndexes.getOrDefault("mainhand", -1);
                if (mainhandSlotIndex != -1)
                {
                    for (var entry : greenAttributes.entrySet())
                    {
                        var attrName = entry.getKey();
                        var value = entry.getValue();
                        var line = Text.literal(" ").append(
                            Text.translatable(
                                "attribute.modifier.equals.0",
                                Text.literal(ItemStack.MODIFIER_FORMAT.format(value)).formatted(Formatting.DARK_GREEN),
                                Text.translatable(attrName).formatted(Formatting.DARK_GREEN)
                            )
                        );
                        lines.add(mainhandSlotIndex + i + 1, line);
                        i++;
                    }
                }
            }
            catch (Exception ex)
            {
                lines.clear();
                lines.addAll(original);
            }
        });


        ClientPlayNetworking.registerGlobalReceiver(AttributeSetter.PACKET_ID, (client, handler, buf, responseSender) -> {
            AttributeSetter.entityEntries.clear();
            AttributeSetter.itemEntries.clear();
            AttributeSetterAPI.clearAll();
            int entitySize = buf.readInt();
            for (int i = 0; i < entitySize; i++)
            {
                String fName = buf.readString();
                var obj = (JsonObject) JsonParser.parseString(buf.readString());
                AttributeSetter.entityEntries.put(fName, obj);
                AttributeSetter.handleEntityJson(fName, obj);
            }
            int itemSize = buf.readInt();
            for (int i = 0; i < itemSize; i++)
            {
                String fName = buf.readString();
                var obj = (JsonObject) JsonParser.parseString(buf.readString());
                AttributeSetter.itemEntries.put(fName, obj);
                AttributeSetter.handleItemJson(fName, obj);
            }
        });
    }
}
