package com.httpedro.attributesetter;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vanilla renders one tooltip line per attribute modifier. Because AttributeSetter injects extra
 * modifiers through {@link net.neoforged.neoforge.event.ItemAttributeModifierEvent}, an item can end
 * up showing several lines for the same attribute (e.g. the item's own "+3 Armor" and AS's "+2 Armor")
 * and separate blue lines for main-hand attack damage/speed that vanilla would otherwise fold into the
 * green summary line.
 * <p>
 * Rather than scraping the already-localized tooltip text, this rebuilds the attribute section from the
 * real modifier data ({@link ItemStack#getAttributeModifiers()}), merging modifiers that share an
 * attribute + slot group + operation and folding all main-hand attack damage/speed into a single green
 * total. The output matches vanilla formatting exactly, so it is indistinguishable from a vanilla item
 * that happened to have those combined modifiers.
 */
public class AttributeSetterClient {

    /** Player base values used when no player is available to query (e.g. a JEI/REI render). */
    private static final double DEFAULT_ATTACK_DAMAGE = 1.0;
    private static final double DEFAULT_ATTACK_SPEED = 4.0;

    public static void mergeTooltips(ItemTooltipEvent e)
    {
        if (Attributesetter.isApothic)
            return;

        List<Component> lines = e.getToolTip();
        List<Component> original = new ArrayList<>(lines);
        try
        {
            int start = firstAttributeHeader(lines);
            if (start < 0)
                return; // item has no attribute section to merge

            // Include the blank separator vanilla inserts before the first slot-group header.
            int from = (start > 0 && isBlank(lines.get(start - 1))) ? start - 1 : start;
            int to = attributeSectionEnd(lines, from);

            List<Component> merged = buildMergedSection(e.getItemStack(), e.getEntity());

            lines.subList(from, to).clear();
            lines.addAll(from, merged);
        }
        catch (Exception ex)
        {
            // Never break another mod's tooltip: restore whatever was there before we touched it.
            lines.clear();
            lines.addAll(original);
        }
    }

    /** Rebuilds the whole "When in Main Hand: ..." block(s) from real modifier data. */
    private static List<Component> buildMergedSection(ItemStack stack, Player player)
    {
        // add + multBase + multTotal totals for main-hand attack damage/speed, folded into one green line.
        Map<EquipmentSlotGroup, Map<Holder<Attribute>, double[]>> attackFold = new EnumMap<>(EquipmentSlotGroup.class);
        // per slot group: (attribute, operation) -> summed amount, for every other modifier.
        Map<EquipmentSlotGroup, Map<MergeKey, Double>> summed = new EnumMap<>(EquipmentSlotGroup.class);

        // getAttributeModifiers() fires ItemAttributeModifierEvent once and yields exactly the set
        // vanilla rendered (item defaults + AttributeSetter's additions).
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers())
        {
            EquipmentSlotGroup group = entry.slot();
            Holder<Attribute> attribute = entry.attribute();
            AttributeModifier modifier = entry.modifier();
            if (group == EquipmentSlotGroup.MAINHAND && isAttackAttribute(attribute))
            {
                attackFold.computeIfAbsent(group, g -> new LinkedHashMap<>())
                        .computeIfAbsent(attribute, a -> new double[3])[modifier.operation().id()] += modifier.amount();
            }
            else
            {
                summed.computeIfAbsent(group, g -> new LinkedHashMap<>())
                        .merge(new MergeKey(attribute, modifier.operation().id()), modifier.amount(), Double::sum);
            }
        }

        List<Component> out = new ArrayList<>();
        for (EquipmentSlotGroup group : EquipmentSlotGroup.values())
        {
            List<Component> groupLines = new ArrayList<>();

            Map<Holder<Attribute>, double[]> fold = attackFold.get(group);
            if (fold != null)
                for (var entry : fold.entrySet())
                {
                    double[] totals = entry.getValue();
                    double base = baseValue(entry.getKey(), player);
                    double total = (base + totals[0]) * (1.0 + totals[1]) * (1.0 + totals[2]);
                    groupLines.add(greenLine(entry.getKey(), total));
                }

            Map<MergeKey, Double> sums = summed.get(group);
            if (sums != null)
                for (var entry : sums.entrySet())
                {
                    double amount = entry.getValue();
                    if (amount == 0.0)
                        continue;
                    groupLines.add(plusOrTakeLine(entry.getKey().attribute(), entry.getKey().operation(), amount));
                }

            if (!groupLines.isEmpty())
            {
                out.add(Component.empty());
                out.add(Component.translatable("item.modifiers." + group.getSerializedName()).withStyle(ChatFormatting.GRAY));
                out.addAll(groupLines);
            }
        }
        return out;
    }

    /** A folded main-hand attack line: " 7 Attack Damage" in dark green, matching vanilla. */
    private static Component greenLine(Holder<Attribute> attribute, double value)
    {
        return Component.literal(" ").append(Component.translatable(
                "attribute.modifier.equals.0",
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value),
                Component.translatable(attribute.value().getDescriptionId())
        )).withStyle(ChatFormatting.DARK_GREEN);
    }

    /** A regular blue/red modifier line, formatted exactly as vanilla's addModifierTooltip. */
    private static Component plusOrTakeLine(Holder<Attribute> attribute, int operation, double amount)
    {
        double display;
        if (operation == 1 || operation == 2) // ADD_MULTIPLIED_BASE / ADD_MULTIPLIED_TOTAL
            display = amount * 100.0;
        else if (attribute.value() == Attributes.KNOCKBACK_RESISTANCE.value())
            display = amount * 10.0;
        else
            display = amount;

        boolean positive = amount > 0.0;
        String key = (positive ? "attribute.modifier.plus." : "attribute.modifier.take.") + operation;
        return Component.translatable(
                key,
                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(display)),
                Component.translatable(attribute.value().getDescriptionId())
        ).withStyle(attribute.value().getStyle(positive));
    }

    private static boolean isAttackAttribute(Holder<Attribute> attribute)
    {
        return attribute.value() == Attributes.ATTACK_DAMAGE.value() || attribute.value() == Attributes.ATTACK_SPEED.value();
    }

    private static double baseValue(Holder<Attribute> attribute, Player player)
    {
        if (player != null)
            return player.getAttributeBaseValue(attribute);
        return attribute.value() == Attributes.ATTACK_SPEED.value() ? DEFAULT_ATTACK_SPEED : DEFAULT_ATTACK_DAMAGE;
    }

    // --- tooltip line scanning -------------------------------------------------------------------

    private static int firstAttributeHeader(List<Component> lines)
    {
        for (int i = 0; i < lines.size(); i++)
            if (isSlotHeader(lines.get(i)))
                return i;
        return -1;
    }

    /**
     * Given the index of the leading blank/header, returns the exclusive end of the attribute section,
     * consuming consecutive {@code [blank] header modifier...} groups without eating a trailing blank
     * that belongs to whatever section follows.
     */
    private static int attributeSectionEnd(List<Component> lines, int from)
    {
        int i = from;
        while (i < lines.size())
        {
            int j = i;
            if (isBlank(lines.get(j)))
                j++;
            if (j >= lines.size() || !isSlotHeader(lines.get(j)))
                break;
            j++; // header
            while (j < lines.size() && isModifierLine(lines.get(j)) && !isSlotHeader(lines.get(j)))
                j++;
            i = j;
        }
        return i;
    }

    private static boolean isSlotHeader(Component line)
    {
        return line.getContents() instanceof TranslatableContents ttc && ttc.getKey().startsWith("item.modifiers.");
    }

    /**
     * Translation key prefixes used by attribute modifier lines. NeoForge replaces vanilla's attribute tooltip
     * section with its own ({@code AttributeUtil.addAttributeTooltips}), which renders non-base modifiers as
     * {@code neoforge.modifier.plus} / {@code neoforge.modifier.take} instead of {@code attribute.modifier.plus.N}.
     * Missing those keys made the section scan stop early and leave the original lines below our merged block.
     */
    private static final String[] MODIFIER_KEY_PREFIXES = {
            "item.modifiers.",
            "attribute.modifier.",
            "neoforge.modifier.",
            "neoforge.attribute.debug."
    };

    private static boolean isModifierLine(Component line)
    {
        // Base lines are a literal " " (and NeoForge's shift-expanded rows a literal " ┇ ") with the actual
        // modifier translatable appended as a sibling, so the whole tree has to be checked, not just the root.
        return hasModifierKey(line);
    }

    private static boolean hasModifierKey(Component component)
    {
        if (component.getContents() instanceof TranslatableContents ttc)
        {
            String key = ttc.getKey();
            for (String prefix : MODIFIER_KEY_PREFIXES)
                if (key.startsWith(prefix))
                    return true;
        }
        for (Component sibling : component.getSiblings())
            if (hasModifierKey(sibling))
                return true;
        return false;
    }

    private static boolean isBlank(Component line)
    {
        return line.getString().isEmpty();
    }

    private record MergeKey(Holder<Attribute> attribute, int operation) {}
}
