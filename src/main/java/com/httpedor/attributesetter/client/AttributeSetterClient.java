package com.httpedor.attributesetter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AttributeSetterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, ctx, lines) -> {
            Map<String, Double> greenAttributes = new HashMap<>();
            String currentSlot = null;
            int mainhandSlotIndex = -1;
            int i = 0;
            for (Iterator<Text> it = lines.iterator(); it.hasNext();)
            {
                var line = it.next();
                var content = line.getContent();
                //Normal attr modifiers
                if (content instanceof TranslatableTextContent ttc)
                {
                    if (ttc.getKey().startsWith("item.modifiers"))
                    {
                        currentSlot = ttc.getKey().substring(ttc.getKey().lastIndexOf('.')+1);
                        if (currentSlot.equals("mainhand"))
                            mainhandSlotIndex = i;
                    }

                    else if (ttc.getKey().startsWith("attribute.modifier.plus.0") && currentSlot != null)
                    {
                        var attrName = ((TranslatableTextContent)((MutableText)ttc.getArg(1)).getContent()).getKey();
                        if (greenAttributes.containsKey(attrName))
                        {
                            greenAttributes.put(attrName, greenAttributes.get(attrName) + Double.parseDouble(ttc.getArg(0).getString()));
                            it.remove();
                        }
                        else if ((attrName.equals("attribute.name.generic.attack_damage") || attrName.equals("attribute.name.generic.attack_speed")) && currentSlot.equals("mainhand"))
                        {
                            greenAttributes.put(attrName, Double.parseDouble(ttc.getArg(0).getString()));
                            it.remove();
                        }
                    }
                }
                //Green attr
                else
                {
                    for (var part : line.getSiblings())
                    {
                        if (part.getContent() instanceof TranslatableTextContent ttc && ttc.getKey().startsWith("attribute.modifier.equals.0"))
                        {
                            var attrName = ((TranslatableTextContent)((MutableText)ttc.getArg(1)).getContent()).getKey();
                            greenAttributes.put(attrName, Double.parseDouble(ttc.getArg(0).getString()));
                            it.remove();
                        }
                    }
                }
                i++;
            }
            i = 0;
            for (var entry : greenAttributes.entrySet())
            {
                var attrName = entry.getKey();
                var value = entry.getValue();
                var line = Text.literal(" ").append(Text.translatable("attribute.modifier.equals.0", Text.literal(ItemStack.MODIFIER_FORMAT.format(value)).formatted(Formatting.DARK_GREEN), Text.translatable(attrName).formatted(Formatting.DARK_GREEN)));
                lines.add(mainhandSlotIndex + i + 1, line);
                i++;
            }
        });
    }
}
