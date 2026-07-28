# AttributeSetter

AttributeSetter Is a simple lightweight mod that can change the default attributes of any living entity through datapacks, inspired by [Jackiecrazy's Attributizer](https://github.com/Jackiecrazy/attributizer)


# How To Use
If you have any questions, you can visit the [Discord](https://discord.gg/fNp78qTqW7)
## Entities
Inside your datapack namespace folder, create a `attributesetter\entity` folder, and inside it, you can put as many json files as you want, with this format:
```json5
{
  "minecraft:creeper": [
    {
      "attribute": "minecraft:generic.max_health",
      "uuid": "0e1c07ef-d456-4567-b748-96b6f84b409e", //optional
      "value": 5,
      "operation": "BASE" // optional, default operation is BASE
    },
    {
      "attribute": "minecraft:generic.follow_range",
      "value": 10,
      "operation": "ADDITION"
    }
  ],
  "#minecraft:raiders": [
    {
      "attribute": "minecraft:generic.max_health",
      "value": 8,
      "operation": "ADDITION"
    }
  ],
  "anymob": [ //This expands to "example:anymob" because of the filename
    {
      "attribute": "projectile_damage:generic",
      "value": 10,
      //default operation is BASE
    }
  ]
}
```
This file should be at `data/example/attributesetter/entity/example.json`
**IMPORTANT**: REMOVE ALL THE COMMENTS (THE LINES THAT STARTS WITH //) BEFORE USING THE JSON IN YOUR DATAPACK, THEY ARE JUST FOR EXPLANATION PURPOSES AND WILL CRASH YOUR DATAPACK*

In the example above, all creepers will have 5 health, and +10 follow range. All entities tagged as raiders will have +8 health. The mob "example:anymob" will have 10 projectile damage.
### Object Key
Which entity ID will be changed. If the first character is a # the key is treated as a tag. In the example above, all entities tagged as raiders will have +8 health, and all creepers will have 5 health. If no namespace is provided, it uses the filename. For example, if I'm in file "alexsmobs.json", and I'm editing entity "void_worm", instead of typing "alexsmobs:voidworm", I can just type "voidworm"
**NEW**: You can use regex to match entity names with the regex prefix. For example, to match all items that have "skeleton" in their id, you can use `regex:.*skeleton.*`
You can use `!` at the start of the key to negate it. For example: `!minecraft:zombie` would apply the attribute to all entities except zombies.
You can use the `&&` and `||` operators to combine multiple selectors. For example: `minecraft:skeleton || minecraft:stray` would apply the attribute to both skeletons and strays, while `#minecraft:undead && !minecraft:stray` would apply the attribute to all undead except strays.
You can use NBT selectors to apply attributes only to items with specific NBT data. For example, you can target all turtles with an egg like this:
```json5
{
  "{HasEgg:true}": [
    {
      "attribute": "minecraft:generic.max_health",
      "value": 10,
      "operation": "ADDITION",
      "slot": "mainhand"
    }
  ],
  "minecraft:zombie{IsBaby:1b}": [
    {
      "attribute": "minecraft:generic.movement_speed",
      "value": 0.2,
      "operation": "MULTIPLY_TOTAL"
    }
  ]
}
```

This would increase the max health of all turtles with an egg by 10 and increase the movement speed of baby zombies by 20%.
Keep in mind that NBT selectors are only checked when the entity is first loaded, so if you add or remove the NBT data after that, the attributes won't update.

### Attribute
Which attribute should be changed, supports modded attributes.

### Operation
Can be `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL`, and `BASE`. The first tree are explained in the [MC Wiki](https://minecraft.fandom.com/wiki/Attribute#Operations), and BASE means it will override the default base value for that attribute. Default is BASE

## Items
Inside your datapack namespace folder, create a `attributesetter\item` folder, and inside it you can put as many json files as you want, with this format:

```json5
{
  "minecraft:stick": [
    {
      "attribute": "minecraft:generic.attack_damage",
      "uuid": "0e1c07ef-d456-4567-b748-96b6f84b409e", //optional, but you should generate one if you are adding more than one modifier
      "value": 5,
      "operation": "ADDITION", //Optional, default value is ADDITION
      "slot": "mainhand" //Optional, default value is the appropriate slot if it's an armor, or mainhand if it's not. Supports CuriosAPI(not trinkets)
    },
    {
      "attribute": "minecraft:generic.max_health",
      "value": 1,
      "operation": "MULTIPLY_TOTAL",
      "slot": "offhand"
    }
  ],
  "#c:swords": [
    {
      "attribute": "minecraft:generic.max_health",
      "value": 8,
      "operation": "ADDITION"
    }
  ],
  "minecraft:diamond_chestplate": [
    {
      "attribute": "minecraft:generic.max_health",
      "value": 10
      //Don't need the 'slot', it recognizes the item is equipable only in the chestplate slot and assigns the correct slot
      //If you want to, you can still override the slot
    }
  ],
  "necklace": [ //This expands to "example:necklace" because of the filename
    {
      "attribute": "minecraft:generic.attack_damage",
      "value": 0.5,
      "operation": "MULTIPLY_BASE",
      "slot": "curio:necklace" //Curios slot
    }
  ]
}
```
This file should be at `data/example/attributesetter/item/example.json`

In the example above, all swords have +8 health, and all sticks will deal +5 damage if in the main hand, and 2x health if in the offhand.
### Object Key
Which item ID will be changed.
If the first character is a # the key is treated as a tag.
If no namespace is provided, it uses the filename. For example, if I'm in file "alexsmobs.json", and I'm editing item "emu_leggings", instead of typing "alexsmobs:emu_leggings", I can just type "emu_leggings".
**NEW**: You can use regex to match item names with the regex prefix. For example, to match all items that have "diamond_" in their id, you can use `regex:.*diamond_.*`
You can use `!` at the start of the key to negate it. For example: `!minecraft:diamond_sword` would apply the attribute to all items except diamond swords.
You can use the `&&` and `||` operators to combine multiple selectors. For example: `minecraft:iron_sword || minecraft:stone_sword` would apply the attribute to both iron and stone swords, while `#minecraft:swords && !minecraft:stone_sword` would apply the attribute to all swords except stone swords.
You can use NBT selectors to apply attributes only to items with specific NBT data. For example, you can target all sharpness 5 swords like this:
```json5
{
  "{tag:{Enchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}}": [
    {
      "attribute": "minecraft:generic.attack_damage",
      "value": 10,
      "operation": "ADDITION",
      "slot": "mainhand"
    }
  ],
  "minecraft:golden_sword{tag:{Enchantments:[{id:\"minecraft:sharpness\",lvl:5s}]}}": [
    {
      "attribute": "minecraft:generic.attack_damage",
      "value": 10,
      "operation": "ADDITION",
      "slot": "mainhand"
    }
  ]
}
```
This would increase the attack damage of all sharpness 5 swords by 10, and increase the attack damage of golden swords with sharpness 5 by another 10.
To see what NBT an item has, you can use the `/data get` command. For example, to see the NBT of the item in your main hand, you can use `/data get entity @s SelectedItem`

### UUID
This is how minecraft knows which item has which modifier. ~~If you only have one modifier in the item you can ignore this, but if you have more than one, you should generate a UUID for each one. You can use [this site](https://www.uuidgenerator.net/) to generate one. BASE operation doesn't need a UUID~~. This is no longer necessary as of update 1.11

### Attribute
Which attribute should be changed, supports modded attributes.

### Operation
Can be `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL`, `BASE`, `DURABILITY`, `DEPENDENCY`, `CONVERSION`. The first three are in the [MC Wiki](https://minecraft.fandom.com/wiki/Attribute#Operations). 
`BASE` removes all other modifiers for that attribute and sets the value to the one in the json file.
`DURABILITY` is a special operation that only works with items that can break. It overrides the item's durability to the value in the json file.
Default is ADDITION
**NEW**: `DEPENDENCY` sets the attribute value based on another attribute. It requires two additional fields: `dependency`, which is the attribute to base the value on, and `multiplier`, which is the value to multiply the dependency attribute by. For example, if you want to set the attack damage of an item to be double that item's attack speed, you would use:
```json5
{
  "minecraft:example_item": [
    {
      "attribute": "minecraft:generic.attack_damage",
      "operation": "DEPENDENCY",
      "dependency": "minecraft:generic.attack_speed",
      "multiplier": 2.0
    }
  ]
}
```
This is useful for when you are changing many items with the same selector. For example, I can make all chestplates give more health, and instead of calculating the exact value for each chestplate, I can just set the health to be 5 times the armor value of the chestplate:
```json5
{
  "#minecraft:chestplates": [
    {
      "attribute": "minecraft:generic.max_health",
      "operation": "DEPENDENCY",
      "dependency": "minecraft:generic.armor",
      "multiplier": 5.0
    }
  ]
}
```
**NEW**: `CONVERSION` transforms one attribute into another. It requires three additional fields: `from`, which is the attribute to convert from, `amount`, which is how much of the `from` attribute should be converted, and `rate`, which is the conversion rate. For example, if you want to remove all armor in the game and convert it into health, at a rate of 1 armor point = 2 health points, you would use:
```json5
{
  "#minecraft:armor": [
    {
      "attribute": "minecraft:generic.max_health",
      "operation": "CONVERSION",
      "from": "minecraft:generic.armor",
      "amount": 1.0, //100% of the armor value will be converted
      "rate": 2.0 //each armor point will be converted into 2 health points
    }
  ]
}
```

### Slot
Can be mainhand, offhand, head, chest, legs, feet, or any Curios slot if prefixed with "curio:". Default value is mainhand,
If the item's class extends ArmorItem(all armor items in the game do), the default value is based on the armor slot. This means that for most armors, you don't have to specify the slot.

## Special selectors
On top of IDs, tags, regex, NBT and the `!`/`&&`/`||` operators, you can use these keyword selectors as the object key. They are cached per item/entity type, so they are very cheap even when matching every item in the game.

**Entity selectors**
- `isEnemy` — any hostile mob (implements the `Enemy` interface).
- `isMonster`, `isCreature`, `isMisc`, `isWaterCreature` — matches the entity's spawn category (`MobCategory`).

**Item selectors**
- `isFood` — any edible item.
- `hasDurability` — any item that takes durability damage.
- `isEnchanted` — any stack that currently has enchantments (checked per stack).
- `isPotion` — any stack that carries a potion (potions, splash/lingering potions, tipped arrows).

Example — give every hostile mob 2x max health:
```json5
{
  "isEnemy": [
    { "attribute": "minecraft:generic.max_health", "value": 2.0, "operation": "MULTIPLY_BASE" }
  ]
}
```

## Operation aliases
Everywhere an `operation` is accepted you can now also use these aliases: `+`/`add`/`add_value` (→ `ADDITION`), `%`/`percent`/`add_multiplied_base` (→ `MULTIPLY_BASE`), and `*`/`x`/`add_multiplied_total` (→ `MULTIPLY_TOTAL`). This keeps datapacks written for the 1.21 version compatible.

## More item operations
These operations don't take an `attribute`; they change the item itself (globally, for every stack of that item). They are re-applied on `/reload` and reverted when their entry is removed.

- `max_stack` — set an item's max stack size. Use `value` for an absolute size or `multiplier` to scale the current one.
- `durability` — set max durability. Now also supports `multiplier`.
- `food` — turn an item into (or replace its) food. Fields: `nutrition` (int) and `saturation` (float) are required; optional `can_always_eat`, `fast_food`, `eat_seconds`, `converts_to` (item id it becomes when eaten), and `effects` (array of `{ "effect": <id>, "duration": <ticks>, "amplifier": <int, optional>, "chance": <0..1, optional> }`). Set `"remove": true` to strip food properties from an item.
- `food_modify` — scale/offset an item's existing food values. Fields: `nutrition_multiplier`, `saturation_multiplier`, `eat_seconds_mult`, `nutrition_offset`, `saturation_offset`, `eat_seconds_offset` (all optional).
- `tooltip_add` — append lines to an item's tooltip. Field `tooltip` (or `components`) is a string or an array of strings / raw JSON text components.
- `tooltip_modify` — `INSERT`, `REPLACE` or `REMOVE` tooltip lines. Fields: `type`, `index`, and `components`/`tooltip`.

```json5
{
  "minecraft:apple": [
    {
      "operation": "food",
      "nutrition": 8,
      "saturation": 1.0,
      "eat_seconds": 0.8,
      "effects": [ { "effect": "minecraft:regeneration", "duration": 100, "amplifier": 1, "chance": 1.0 } ]
    },
    { "operation": "max_stack", "value": 16 },
    { "operation": "tooltip_add", "tooltip": "§6A magic apple" }
  ]
}
```

## Removing things from the game
`operation: "remove"` (alias `delete`) takes whatever the selector matches out of the game entirely. It works in both the `item` and the `entity` folder and takes no other field.

```json5
// attributesetter/item/<pack>.json
{
  "minecraft:diamond_sword": [ { "operation": "remove" } ],
  "regex:.*:.*_horse_armor": [ { "operation": "remove" } ]
}
```

```json5
// attributesetter/entity/<pack>.json
{
  "minecraft:phantom": [ { "operation": "remove" } ],
  "#minecraft:raiders": [ { "operation": "delete" } ]
}
```

A removed **item** disappears from:
- the creative inventory and the creative search — and therefore from JEI, which builds its item list from the creative tabs;
- every recipe: recipes that produce it are dropped, and so are recipes that can no longer be crafted because one of their ingredients only accepted removed items (a tag ingredient that lost a single item is kept);
- every loot table — block drops, mob drops, chests, fishing, gifts — since removed items are filtered out of each loot roll;
- villager and wandering trader offers that buy or sell it;
- the world: dropped item entities are deleted as they spawn;
- player inventories and ender chests, which are swept once a second, so copies that already existed when the datapack changed go away too.

A removed **entity** never joins a level, which covers natural spawns, spawners, spawn eggs, breeding and entities already saved in a chunk. On top of that, its spawn egg is removed like any other removed item, and the spawn placement check denies it early so it doesn't eat the mob cap. Players are never removed, whatever the selector matches.

Removals are recomputed from scratch on every `/reload`, so deleting the entry brings everything back — recipes and loot tables were never rewritten, only filtered. The two exceptions are things that are gone for good: trades already stripped off a villager, and items already swept out of an inventory.

Some things are deliberately out of scope: removing a block item does not remove the block itself, and a removed item can still be produced by `/give` or by mod code (it will be swept out of the player's inventory shortly after).

## Entity: creeper explosion power
Use `operation: "creeper_explosion_power"` (alias `explosion_power`) on a creeper selector with either `power` (absolute) or `multiplier` (scale the current radius).

## Attributes (attribute merging)
Create an `attributesetter/attribute` folder to make one attribute feed off another. The object key selects the target attribute (by id, regex, `!`, `&&`/`||`). Each entry uses `operation: "inject"` with:
- `source` — the other attribute's id.
- `type` — `INJECT` (source gets the target's modifiers), `COPY` (target gets the source's modifiers) or `BIDIRECTIONAL` (both).
- `multiplier` — optional factor applied to the copied values (default 1.0).
- `operations` — optional list of operations to mirror (defaults to all).

```json5
{
  "minecraft:generic.attack_damage": [
    { "operation": "inject", "source": "minecraft:generic.attack_speed", "type": "COPY", "multiplier": 0.5 }
  ]
}
```
This makes attack damage also gain half of whatever modifies attack speed. Injections are recomputed live, so changing the source attribute updates the target automatically.