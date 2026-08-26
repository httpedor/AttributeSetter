# AttributeSetter Usage Guide

This guide documents the datapack format and selectors based on the current builders in
`Attributesetter.setupSelectors`, `Attributesetter.setupSetters`, `Attributesetter.setupSetterShorthands`, and
`CuriosCompat`.

## Datapack Structure

All data lives under the `attributesetter` root inside a datapack:

```
data/<namespace>/attributesetter/<folder>/<file>.json
```

Each `<folder>` picks what the file is about:

- `entity` (living entities)
- `item` (everything about items: attribute modifiers, tooltips, durability, food, stack size, ...)
- `block` (hardness, blast resistance, mining speed, remove/replace)
- `recipe` (remove a recipe, or rewrite its result / ingredients)
- `attribute` (attribute-to-attribute injections)

`item_type` still works and still means "item-level only", but you no longer need it: the `item` folder
accepts both the per-stack operations (attribute modifiers, tooltips) and the per-item ones (durability, food,
stack size). Each setter is handed to whichever of the two understands it. Plurals also work as folder names
(`entities`, `items`, `blocks`, `recipes`, `attributes`).

### File layout

The usual layout is an object whose keys are selectors:

```json
{
  "minecraft:creeper": [
    { "attribute": "minecraft:generic.max_health", "value": 5, "operation": "BASE" }
  ]
}
```

Three shortenings apply everywhere a list of setters is expected — the value on a plain selector key, and the
`setters` field of an entry object below:

- a single setter does not need to be wrapped in an array — `"minecraft:creeper": { ... }`,
- a setter can be written as a string — `"minecraft:stone_sword": "remove"` (see [Setter shorthands](#setter-shorthands)),
- a selector can be a JSON object rather than a string (see [Selectors](#selectors)).

Because JSON keys have to be strings, a selector written as an object needs somewhere else to live. Either give
the entry a `selector` field, in which case the key becomes a plain label:

```json
{
  "cheap ores": {
    "selector": { "tag": "c:ores", "namespace": "minecraft" },
    "setters": [ "hardness x0.5" ]
  }
}
```

or write the whole file as an array of entries:

```json
[
  {
    "selector": { "any": ["#c:ores", "regex:.*_ore"] },
    "setters": "hardness x0.5"
  },
  {
    "name": "flat obsidian",
    "selector": "minecraft:obsidian",
    "setters": { "operation": "hardness", "value": 5 }
  }
]
```

`selector` also accepts `select` / `target`, and `setters` also accepts `apply` / `modifiers` / `operations`.
`setters` follows the same "single value doesn't need an array" rule as everything else — a lone setter object
(`{"operation": "hardness", "value": 5}`) works exactly like `[{"operation": "hardness", "value": 5}]`, whether
it's the full object form or the shorthand string.

An entry written this way can also carry a `name` (or `id`), used as its label in modifier ids and log lines
instead of the array index.

Keys starting with an underscore are ignored, so `"_comment"` can be used as a note to yourself.

The filename provides the default namespace for selectors that omit one.

## Selectors

A selector can be written three ways, and they mix freely:

- **a string** — the shorthand, best for the common cases (`"minecraft:zombie"`, `"#c:ores"`, `"!isFood"`),
- **an object** — the full form, for anything the shorthand cannot express,
- **an array** — matches if *any* entry matches (`["minecraft:zombie", "#minecraft:skeletons"]`).

Every selector below is available on every target type that can support it, worked out from what the target
actually is rather than listed per folder. And because an item stack *is* a specific item, every item selector
(id, tag, regex, namespace, `isFood`, ...) works on item stacks too, with no extra work.

### The object form

An object with a `type` field is one selector spelled out in full:

```json
{ "type": "regex", "pattern": ".*_SWORD", "ignore_case": true, "match": "path" }
```

An object *without* a `type` is read as "all of these at once":

```json
{ "namespace": "minecraft", "tag": "c:swords", "nbt": { "Damage": 0 } }
```

Two fields work on any selector object:

- `"invert": true` — flips it,
- `"specificity": 120` — overrides how specific it counts as. Entries are applied least-specific first, so a
  higher number makes this entry win over the ones it overlaps with.

### Available selectors

| Written as a string | Written as an object | Matches |
| --- | --- | --- |
| `minecraft:zombie` | `{"id": "minecraft:zombie"}`, `{"id": ["a", "b"]}` | one specific id. No namespace means the filename is used |
| `#minecraft:skeletons` | `{"tag": "minecraft:skeletons"}`, `{"tag": [...]}` | anything in that tag |
| `regex:.*skeleton.*` | `{"regex": ".*skeleton.*"}` | the id against a regex |
| `@somemod` | `{"namespace": "somemod"}` | everything that mod added |
| `*` | `{"type": "always"}` | everything |
| `{IsBaby:1b}`, `minecraft:zombie{IsBaby:1b}` | `{"nbt": {"IsBaby": 1}}` | NBT, for targets that have any |
| `isFood`, `hasDurability`, `isEnchanted`, `isPotion`, `isTool`, `isDyeable`, `isFireResistant`, `hasAttributes` | `{"isFood": true}` | items carrying that data component |
| `component:minecraft:food` | `{"has_component": "minecraft:food"}`, `{"has_component": [...]}` | any data component by id; a list means "has all of them" |
| `isMonster`, `isCreature`, `isAmbient`, `isWaterCreature`, ... | `{"mob_category": "monster"}`, `{"mob_category": [...]}` | entities in that spawn category. Every vanilla category has a word |
| `isEnemy` | `{"is_enemy": true}` | entities the game treats as hostile |
| `type:minecraft:smelting` | `{"recipe_type": "minecraft:smelting"}` | recipes of that recipe type (recipes only) |
| — | `{"recipe_type_regex": ".*cooking"}` | recipes whose type id matches a regex (recipes only) |
| `ingredient:#minecraft:planks` | `{"contains_ingredient": "#minecraft:planks"}` | recipes that accept a matching item (recipes only) |
| `result:minecraft:torch` | `{"contains_result": "minecraft:torch"}` | recipes whose result is a matching item (recipes only) |
| `!minecraft:zombie` | `{"not": "minecraft:zombie"}` | anything the inner selector does not |
| `a \|\| b` | `{"any": ["a", "b"]}` | either |
| `a && b` | `{"all": ["a", "b"]}` | both |

`regex` takes two extra options: `"ignore_case": true`, and `"match"` set to `full` (the default), `path` or
`namespace` to pick which part of the id the pattern runs against.

The recipe-only `contains_ingredient` / `contains_result` matchers take a full **item** selector as their value,
so anything you can write to pick an item — an id, `#tag`, `regex:`, a nested `any`/`all` — works to pick which
recipes to touch (`{"contains_ingredient": {"regex": ".*_log"}}`). The plain `id`/`regex`/`namespace` selectors
on the recipe target match against the recipe's own id.

`not`, `any` and `all` nest as deep as you like, and each of their entries is itself a full selector — string,
object or array:

```json
{
  "all": [
    "#c:swords",
    { "not": { "any": ["@somemod", "regex:.*_wooden_.*"] } },
    { "nbt": { "Damage": 0 } }
  ]
}
```

NBT is compared as a subset, and can be written either as SNBT (`"{IsBaby:1b}"`) or as a plain JSON object
(`{"IsBaby": 1}`). It is evaluated when the entity or item stack is loaded, not continuously.

## Setter shorthands

Any setter can be written as a string instead of an object:

```json
{
  "minecraft:stone_sword": "remove",
  "minecraft:diamond_sword": ["durability x2", "tooltip Twice the sword"]
}
```

The grammar is `<operation> [token]...`, where each token is one of:

| Token | Becomes |
| --- | --- |
| `key=value` | that field, as written (`slot=chest`, `broadcast=false`) |
| `x2`, `*2` | `"multiplier": 2` |
| `+5`, `-5` | `"offset": 5` |
| anything else | a positional argument, whose meaning depends on the operation |

Double quotes group a token containing spaces. That alone covers most operations:

| Shorthand | Same as |
| --- | --- |
| `remove` / `delete` | `{"operation": "remove"}` |
| `unique` | `{"operation": "unique", "limit": 1}` |
| `unique 3` | `{"operation": "unique", "limit": 3}` |
| `unique_shared 1 broadcast=false` | a pooled cap that stays quiet |
| `durability 500` | a flat durability |
| `durability x2` / `durability +100` | scaled / offset durability |
| `max_stack 16`, `max_stack x2` | stack size |
| `hardness 5`, `hardness x0.5`, `hardness +2` | block hardness, flat or tuned |
| `blast_resistance x3`, `mining_speed x2` | the other two block tuners |
| `explosion_power 5` | creeper blast radius |
| `replace minecraft:stone` | swap the block out |
| `food_modify nutrition_multiplier=2` | tune existing food values |

A handful of operations take arguments that do not fit that pattern, so they read their own way:

| Shorthand | Meaning |
| --- | --- |
| `attribute <id> <amount> [operation]` | `attribute minecraft:generic.max_health 40` |
| `attribute <id> +5` | an additive modifier of 5 |
| `attribute <id> %0.25` | a `multiply_base` modifier of 0.25 |
| `attribute <id> 0.5 multiply_total slot=chest` | spelled out, with a slot |
| `tooltip <the rest of the line>` | adds that line to the tooltip |
| `tooltip_remove <index>` | drops a tooltip line |
| `dependency <attribute> <scales with> x2` | see [Dependency](#dependency) |
| `conversion <from> <to> [amount] [rate]` | see [Conversion](#conversion) |
| `inject <source attribute> x0.5` | see [Attribute injections](#attribute-injections-attributesetterattribute) |
| `remove_food` | strips the food component |
| `replace_result <item>` | see [Recipes](#recipes-attributesetterrecipe) |
| `replace_ingredient <match> <with>` | see [Recipes](#recipes-attributesetterrecipe) |

Written out with no operation of its own, `attribute` keeps each target's own default: a base value on an
entity, an additive modifier on an item stack.

In the object form, the `operation` field may also be written `op`:

```json
{ "minecraft:stone_sword": { "op": "remove" } }
```

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

These entries add attribute modifiers to item stacks and use the `slot` field. They go in the `item` folder,
alongside the item-level entries below.

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

## Item type (`attributesetter/item`, or `attributesetter/item_type`)

These entries modify item-level data (durability, max stack, food) - the item itself rather than one stack of
it. They can go in the plain `item` folder together with everything above; `item_type` is still accepted and
means "only these", which is occasionally useful for keeping files tidy.

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

These entries change block properties. Blocks have no NBT and no data components, so the selectors that read
those are not available here; everything else (id, tag, regex, namespace, `not`/`any`/`all`, `*`) is.

### Hardness

Retunes the block's hardness (`destroySpeed`), which is the base mining time. Uses `multiplier` (default
`1.0`) and `offset` (default `0.0`); the result is `current * multiplier + offset`, clamped to `>= 0`. Blocks
vanilla marks unbreakable (bedrock, barrier) are skipped, so a multiplier cannot accidentally make them
diggable.

Give a `value` instead of a multiplier/offset to set the hardness outright - `"hardness 5"` - which *does*
apply to unbreakable blocks, since asking for a number is unambiguous. The same holds for explosion resistance
and mining speed.

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

## Recipes (`attributesetter/recipe`)

Entries in the `recipe` folder (or `recipes`) select recipes and then remove or rewrite them. Recipe edits are
applied to the server's recipe manager during the datapack reload and synced to clients through the game's own
recipe sync, so a `/reload` is enough and removing the entry brings the recipe back.

Recipes are selected by:

- the recipe's own **id** — `minecraft:diamond_sword`, `regex:.*_from_.*`, `@somemod`, and the `!`/`&&`/`||`
  combinators, exactly like every other target;
- **recipe type** — `type:minecraft:smelting` / `{"recipe_type": "minecraft:smelting"}`, or a regex against the
  type id with `{"recipe_type_regex": ".*cooking"}`;
- **an ingredient it accepts** — `ingredient:#minecraft:planks` / `{"contains_ingredient": <item selector>}`;
- **its result** — `result:minecraft:torch` / `{"contains_result": <item selector>}`.

`contains_ingredient` and `contains_result` take a full item selector as their value, so the item side can be a
tag, a regex, or a nested selector.

### Remove

```json
{ "minecraft:diamond_sword": "remove" }
```

- `operation`: `remove` (alias `delete`)

Drops the recipe. Nothing else is needed.

### Replace result

```json
{
  "result:minecraft:torch": {
    "operation": "replace_result",
    "with": "minecraft:soul_torch",
    "count": 4
  }
}
```

- `operation`: `replace_result` (alias `result`)
- `with` (required, alias `to`/`result`/`item`/`value`): the item the recipe now produces. Accepts a full item
  string, so components work (`minecraft:diamond_sword[minecraft:damage=0]`).
- `count` (alias `amount`, default `1`): how many.
- Shorthand: `replace_result minecraft:soul_torch count=4`.

### Replace ingredient

```json
{
  "ingredient:minecraft:stick": {
    "operation": "replace_ingredient",
    "match": "minecraft:stick",
    "with": "minecraft:blaze_rod"
  }
}
```

- `operation`: `replace_ingredient`
- `match` (required, alias `from`/`ingredient`/`target`): an **item selector** — every ingredient of the recipe
  that accepts a matching item is replaced. Same grammar as `contains_ingredient`, so `#minecraft:planks`,
  `{"regex": ".*_log"}`, etc. all work.
- `with` (required, alias `to`/`replacement`): the replacement ingredient — an item id, a `#tag`, or an array of
  either (an array means "any of these").
- Shorthand: `replace_ingredient <match> <with>`, e.g. `replace_ingredient #minecraft:planks minecraft:stone`.

`replace_result` and `replace_ingredient` reconstruct the recipe, so they work on crafting (shaped and
shapeless), smelting/blasting/smoking/campfire, and stonecutting recipes. A recipe type they don't know how to
rebuild (a modded recipe with its own class) is left untouched, with a warning in the log; `remove` works on any
recipe regardless of type.

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

