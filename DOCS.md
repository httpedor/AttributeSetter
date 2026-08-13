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
- `block` (block changes like hardness, blast resistance, mining speed, remove/replace)

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

## Blocks (`attributesetter/block`)

These entries change block properties. Selectors support id, `#tag`, `regex:`, `!`, and `&&`/`||` (no NBT or component selectors).

### Hardness

Retunes the block's hardness (`destroySpeed`), which is the base mining time. Uses `multiplier` (default `1.0`) and `offset` (default `0.0`); the result is `current * multiplier + offset`, clamped to `>= 0`.

```json
{
  "minecraft:obsidian": [
    { "operation": "hardness", "multiplier": 0.5, "offset": 0 }
  ]
}
```

### Explosion resistance

Retunes the block's blast resistance (`explosionResistance`) with the same `multiplier`/`offset` rules.

```json
{
  "#minecraft:base_stone_overworld": [
    { "operation": "explosion_resistance", "offset": 100 }
  ]
}
```

- `operation`: `explosion_resistance` (alias `blast_resistance`)

### Mining speed

Retunes how fast a player digs a matching block, by applying `multiplier`/`offset` to the player's dig speed (via the break-speed event). Unlike hardness this is per-player and dynamic, and it stacks with other break-speed modifiers. Independent of the `hardness` operation.

```json
{
  "regex:.*_ore": [
    { "operation": "mining_speed", "multiplier": 2.0 }
  ]
}
```

- `operation`: `mining_speed` (alias `break_speed`)

### Replace

Swaps the block for another one every time it is generated or placed (worldgen, player/mob/dispenser placement, pistons, fluids). Blocks already sitting in loaded chunks convert only when the game next writes that position.

```json
{
  "minecraft:grass_block": [
    { "operation": "replace", "with": "minecraft:dirt" }
  ]
}
```

- `operation`: `replace`
- `with` (required, alias `to`/`block`): the block to place instead

### Remove

`"operation": "REMOVE"` (alias `DELETE`) also works in the `block` folder: every generated/placed copy becomes air, and the block's BlockItem is removed like any other removed item (see below). Same "already-loaded chunks convert only when re-set" limitation as replace.

## Removing things from the game

`"operation": "REMOVE"` (alias `DELETE`) takes whatever the selector matches out of the game entirely. It takes no other field. Put item removals in the `item` folder (the `item_type` folder is also accepted), entity removals in `entity`, and block removals in `block`. All the usual selectors apply (id, `#tag`, `regex:`, `!`, `&&`/`||`, NBT where supported).

```json
{
  "minecraft:diamond_sword": [ { "operation": "REMOVE" } ],
  "regex:.*:.*_horse_armor": [ { "operation": "REMOVE" } ]
}
```

```json
{
  "minecraft:phantom": [ { "operation": "REMOVE" } ],
  "#minecraft:raiders": [ { "operation": "DELETE" } ]
}
```

A removed **item** disappears from:

- the creative inventory and the creative search — and therefore from JEI, which builds its item list from the creative tabs;
- every recipe: recipes that produce it are dropped, and so are recipes that can no longer be crafted because one of their ingredients only accepted removed items (a tag ingredient that lost a single item is kept);
- every loot table — block drops, mob drops, chests, fishing, gifts — since removed items are filtered out of each loot roll;
- villager and wandering trader offers that buy or sell it;
- the world: dropped item entities are deleted as they spawn;
- player inventories and ender chests, which are swept once a second, so copies that already existed when the datapack changed go away too.

A removed **entity** never joins a level, which covers natural spawns, spawners, spawn eggs, breeding and entities already saved in a chunk. Its spawn egg is removed as an item too, and the spawn placement check denies it early so it doesn't eat the mob cap. Players are never removed, whatever the selector matches.

Removals are recomputed from scratch on every `/reload`, so deleting the entry brings everything back — recipes and loot tables are never rewritten, only filtered. The two exceptions are gone for good: trades already stripped off a villager, and items already swept out of an inventory.

Out of scope: removing a block item does not remove the block itself, and a removed item can still be produced by `/give` or by mod code (it gets swept out of the player's inventory shortly after).

Spawn-egg removal and the early spawn placement check need the selector to be resolvable to entity types on its own. Id, tag, regex, mob-category selectors and their `!`/`&&`/`||` combinations are; NBT and `isEnemy` are not, in which case only those two extras are skipped — the entity is still removed.

## Limiting how many can ever spawn (`make_unique`)

`"operation": "make_unique"` (alias `unique`) caps how many copies of an item or entity may ever be
created in the save. Once the cap is reached, further copies are blocked the same way a removed thing
is — but the item **stays** in the creative tab and JEI, and creative grabs / `/give` do **not**
count. Put item caps in the `item` folder (the `item_type` folder also works) and entity caps in
`entity`. All the usual selectors apply.

```json
{
  "minecraft:diamond_sword": [ { "operation": "make_unique", "limit": 1, "broadcast": true } ]
}
```

```json
{
  "minecraft:ender_dragon": [ { "operation": "make_unique", "limit": 1 } ]
}
```

Fields:

- `limit` (required int, aliases `count`/`max`): how many may be created.
- `broadcast` (optional bool): announce to chat when the cap is reached. Defaults to the
  `broadcastByDefault` server-config option.
- `message` (optional string): custom broadcast message. Placeholders: `%name%`, `%limit%`,
  `%count%`. Falls back to the `defaultBroadcastMessage` server-config value.

With `make_unique`, the limit applies to **each matched type on its own** — `#minecraft:swords`
with `limit: 1` means one of *each* sword type.

### Shared caps (`make_unique_shared`)

`"operation": "make_unique_shared"` (alias `unique_shared`) pools **every type the one entry matched**
into a single counter, so the limit is shared across them:

```json
{
  "minecraft:diamond_sword || minecraft:netherite_sword": [
    { "operation": "make_unique_shared", "limit": 1 }
  ]
}
```

Here only **one** of the two swords can ever exist — creating a diamond sword uses up the shared slot
so no netherite sword can be created, and vice versa. Same fields as `make_unique`.

### What counts, and how it persists

- **Items** count when produced by a **loot table** (mob drops, chests, fishing, gifts) or by a
  **crafting recipe**. `/give`, creative grabs and arbitrary mod code do not count. When the cap is
  hit, loot rolls drop the item and crafting yields nothing (the output is emptied).
- **Entities** count on spawn (natural spawns, spawners, breeding, spawn eggs, `/summon`). Over-cap
  spawns are denied, and each counted unique entity is marked **persistent** so it never despawns.
  Entities that were already saved in a chunk before the cap existed are grandfathered in — they are
  not culled or counted.
- Counting is **lifetime/monotonic per save**: destroying a copy does **not** free a slot. Counts
  live with the world (in the overworld's saved data), so they survive restarts and `/reload`.
  Removing the datapack entry removes the cap but keeps the count — use the commands below to adjust.

### Limitations

- Entity caps resolve their selector to entity types at reload, so — exactly like removal — an
  entity entry that relies on an NBT or `isEnemy` selector alone won't gate; id/tag/regex/mob-category
  selectors and their `!`/`&&`/`||` combinations do.
- A `make_unique_shared` group is keyed by the datapack entry, so heavily renaming/reordering the
  entry starts a fresh shared counter (the commands can migrate it).

### Commands

`/attributesetter unique <item|entity|group> <id> [get | set <n> | add <n> | remove <n> | reset]`
(op / permission level 2) reads and edits the counters. With no verb it prints the current count
(and the active limit, if any). Works on any id whether or not a cap is currently active.

```
/attributesetter unique item minecraft:diamond_sword          # print the count
/attributesetter unique item minecraft:diamond_sword reset    # back to 0
/attributesetter unique entity minecraft:ender_dragon add 1
/attributesetter unique group <groupId> set 0
```

### Server config

`config/attributesetter-server.toml`:

- `defaultBroadcastMessage` — message used when an entry sets `broadcast` but no `message`.
- `broadcastByDefault` — whether entries broadcast when they omit the `broadcast` flag.

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

