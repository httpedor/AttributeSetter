# AttributeSetter Usage Guide

This guide documents the datapack format and selectors based on the current builders in `Attributesetter.setupSelectors`, `Attributesetter.setupSetters`, and `CuriosCompat`.

## Datapack Structure

All data lives under the `attributesetter` root inside a datapack:

```
data/<namespace>/attributesetter/<folder>/<file>.json
```

Each `<folder>` corresponds to a `TargetType` folder name:

- `entity` (living entities)
- `item` (item stacks / attribute modifiers)
- `item_type` (item type changes like durability/food/max stack)
- `attribute` (attribute-to-attribute injections)

Each JSON file is an object where keys are selectors and values are arrays of entries:

```json
{
  "minecraft:creeper": [
    { "attribute": "minecraft:generic.max_health", "value": 5, "operation": "BASE" }
  ]
}
```

Selector keys are parsed using the rules below. The filename provides the default namespace for selectors that omit one.

## Selectors

Selectors are parsed for each target type that supports them. You can combine these features.

### ID selectors

- `minecraft:zombie` targets a specific ID.
- If no namespace is provided, the JSON filename is used as the namespace.

### Tag selectors

- `#minecraft:skeletons` targets a tag.

### Regex selectors

- `regex:.*skeleton.*` matches by regex against the ID string.

### Inverted selectors

- `!minecraft:zombie` matches everything except zombies.
- Works with any selector.

### Composite selectors

- `minecraft:skeleton || minecraft:stray`
- `#minecraft:undead && !minecraft:stray`

### NBT selectors

Works for entity and item stack targets that support NBT serialization:

- `{IsBaby:1b}`
- `minecraft:zombie{IsBaby:1b}`

These are evaluated when the entity or item stack is loaded, not continuously.

### Component selectors (items)

For item and item stack targets:

- `isFood`
- `hasDurability`
- `isEnchanted`
- `isPotion`

## Entity (`attributesetter/entity`)

Supported entries:

### Attribute base or modifiers

```json
{
  "minecraft:creeper": [
    { "attribute": "minecraft:generic.max_health", "value": 5, "operation": "BASE" },
    { "attribute": "minecraft:generic.follow_range", "value": 10, "operation": "ADDITION" }
  ]
}
```

- `attribute` (required)
- `value` (required)
- `operation` (optional): `BASE` (default), `ADDITION` (`ADD`, `+`), `MULTIPLY_BASE` (`PERCENT`, `%`), `MULTIPLY_TOTAL` (`*`, `X`)

### Creeper explosion power

```json
{
  "minecraft:creeper": [
    { "operation": "creeper_explosion_power", "power": 6 }
  ]
}
```

- `operation`: `creeper_explosion_power` or `explosion_power`
- `power` (int) or `multiplier` (float)

## Item stacks (`attributesetter/item`)

These entries add attribute modifiers to item stacks and use the `slot` field.

### Base attribute override

```json
{
  "minecraft:diamond_sword": [
    { "attribute": "minecraft:generic.attack_damage", "value": 10, "operation": "BASE", "slot": "mainhand" }
  ]
}
```

### Attribute modifiers (default)

```json
{
  "minecraft:stick": [
    { "attribute": "minecraft:generic.attack_damage", "value": 5, "operation": "ADDITION", "slot": "mainhand" }
  ]
}
```

- `attribute` (required)
- `value` (required)
- `operation` (optional): same as entity modifiers. If omitted, defaults to `ADDITION`.
- `slot` (optional): `mainhand`, `offhand`, `head`, `chest`, `legs`, `feet`, or `curio:<slot>` (Curios)
  - If omitted and the selector resolves to an armor item, the armor slot is used; otherwise `mainhand`.

### Conversion

```json
{
  "#minecraft:armor": [
    { "operation": "CONVERSION", "attribute": "minecraft:generic.max_health", "from": "minecraft:generic.armor", "amount": 1.0, "rate": 2.0 }
  ]
}
```

- `operation`: `CONVERSION`
- `attribute` (target)
- `from` (source)
- `amount` (fraction of source to convert, default 1.0)
- `rate` (conversion rate, default 1.0)

### Dependency

```json
{
  "#minecraft:chestplates": [
    { "operation": "DEPENDENCY", "attribute": "minecraft:generic.max_health", "dependency": "minecraft:generic.armor", "multiplier": 5.0 }
  ]
}
```

- `operation`: `DEPENDENCY`
- `attribute` (target)
- `dependency` (source)
- `multiplier` (required)

### Tooltip add

Adds one or more tooltip lines/components to matching item stacks.

```json
{
  "minecraft:diamond_sword": [
    {
      "operation": "TOOLTIP_ADD",
      "tooltip": [
        "Simple extra line",
        { "text": "Styled line", "color": "gold", "italic": false }
      ]
    }
  ]
}
```

- `operation`: `TOOLTIP_ADD` (aliases: `TOOLTIPADD`, `TOOLTIP`)
- `tooltip` or `components` (required):
  - string = one literal line
  - array = each element can be a string (literal) or a JSON text component object

### Tooltip modify

Inserts, replaces, or removes tooltip lines/components at a target index.

```json
{
  "minecraft:bow": [
    {
      "operation": "TOOLTIP_MODIFY",
      "type": "REPLACE",
      "index": 1,
      "components": [
        { "text": "Replaced line", "color": "aqua" }
      ]
    },
    {
      "operation": "TOOLTIP_MODIFY",
      "type": "REMOVE",
      "index": 0
    }
  ]
}
```

- `operation`: `TOOLTIP_MODIFY` (alias: `TOOLTIPMODIFY`)
- `type` (optional): enum from `ItemTooltipModifySetter.Type`; defaults to `INSERT`
- `index` (optional): integer index, defaults to `0`
- `components` or `tooltip`:
  - required for non-`REMOVE` types
  - optional for `REMOVE`
  - accepts a single string, or an array of strings/JSON text component objects

## Item type (`attributesetter/item_type`)

These entries modify item-level data (durability, max stack, food).

### Durability

```json
{
  "minecraft:diamond_sword": [
    { "operation": "DURABILITY", "value": 5000 }
  ]
}
```

- `operation`: `DURABILITY`
- `value` (int) or `multiplier` (float)

### Max stack size

```json
{
  "minecraft:ender_pearl": [
    { "operation": "MAX_STACK", "value": 64 }
  ]
}
```

- `operation`: `MAX_STACK`
- `value` (int) or `multiplier` (float)

### Food (override)

```json
{
  "minecraft:golden_carrot": [
    {
      "operation": "FOOD",
      "nutrition": 6,
      "saturation": 14.4,
      "eat_seconds": 1.25,
      "can_always_eat": true,
      "converts_to": "minecraft:carrot",
      "effects": [
        { "type": "minecraft:regeneration", "duration": 200, "amplifier": 1, "chance": 1.0 }
      ]
    }
  ]
}
```

- `operation`: `FOOD`
- `remove` (optional): if true, removes the food component; other fields may be omitted.
- `nutrition` and `saturation` (required unless `remove`)
- `eat_seconds`, `can_always_eat`, `converts_to` (optional)
- `effects` array (optional): `type`, `duration`, `amplifier` (optional), `chance` (optional)

### Food modifiers

```json
{
  "isFood && regex:.*golden.*": [
    {
      "operation": "FOOD_MODIFY",
      "nutrition_mult": 2.0,
      "nutrition_offset": 0,
      "saturation_mult": 0.5,
      "saturation_offset": 5,
      "eat_seconds_mult": 1.0,
      "eat_seconds_offset": 0.5
    }
  ]
}
```

Field names:

- `nutrition_mult` (default 1.0)
- `nutrition_offset` (default 0)
- `saturation_mult` (default 1.0)
- `saturation_offset` (default 0)
- `eat_seconds_mult` (default 1.0)
- `eat_seconds_offset` (default 0)

## Attribute injections (`attributesetter/attribute`)

This target type lets you inject modifiers from one attribute into another.

```json
{
  "minecraft:generic.armor": [
    {
      "operation": "INJECT",
      "source": "minecraft:generic.max_health",
      "multiplier": 1.0,
      "type": "INJECT",
      "operations": ["ADDITION", "MULTIPLY_TOTAL"]
    }
  ]
}
```

- `operation`: `INJECT` (aliases: `INJECTION`, `ATTRIBUTE_INJECTION`)
- `source` (required)
- `multiplier` (optional, default 1.0)
- `type` (optional): value from `AttributeModifiersInjectionSetter.InjectionType`
- `operations` (optional): string or array; parsed with the same operation rules as modifiers

## Curios compatibility

Curios operations use the same `operation`, `attribute`, `value`, and `slot` fields as item stack modifiers, but the slot is prefixed with `curio:`.

```json
{
  "example:necklace": [
    { "attribute": "minecraft:generic.attack_damage", "value": 0.5, "operation": "MULTIPLY_BASE", "slot": "curio:necklace" }
  ]
}
```

If the Curios slot is not provided, the code attempts to infer it from the item, but load order can prevent this. Prefer explicit `curio:<slot>`.

## Notes

- Selector keys that omit a namespace use the JSON filename as the namespace.
- NBT selectors are evaluated when the entity/item stack is loaded.
- Each entry in a selector array receives a unique internal ID based on its file and index.

