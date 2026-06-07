# AttributeSetter

AttributeSetter Is a simple lightweight mod that can change the default attributes of any living entity through datapacks, inspired by [Jackiecrazy's Attributizer](https://github.com/Jackiecrazy/attributizer)


# How To Use
If you have any questions, you can visit the [Discord](https://discord.gg/fNp78qTqW7)

# ATTENTION, THESE ARE OLD DOCS, CHECK THE DOCS.MD FILE FOR THE MOST UP TO DATE DOCUMENTATION.
These docs are only for version 2.1 and below.
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

**IMPORTANT**: Keep in mind that NBT selectors are only checked when the entity is first loaded, so if you add or remove the NBT data after that, the attributes won't update.

### Attribute
Which attribute should be changed, supports modded attributes.

### Operation
Can be `ADD_VALUE`(or `ADDITION`), `ADD_MULTIPLIED_BASE`(or `MULTIPLY_BASE`), `ADD_MULTIPLIED_TOTAL`(or `MULTIPLY_TOTAL`), and `BASE`. The first tree are explained in the [MC Wiki](https://minecraft.fandom.com/wiki/Attribute#Operations), and BASE means it will override the default base value for that attribute. Default is BASE

## Items
Inside your datapack namespace folder, create a `attributesetter\item` folder, and inside it you can put as many json files as you want, with this format:

```json5
{
  "minecraft:stick": [
    {
      "attribute": "minecraft:generic.attack_damage",
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

: You can use regex to match item names with the regex prefix. For example, to match all items that have "diamond_" in their id, you can use `regex:.*diamond_.*`
You can use `!` at the start of the key to negate it. For example: `!minecraft:diamond_sword` would apply the attribute to all items except diamond swords.

You can use the `&&` and `||` operators to combine multiple selectors. For example: `minecraft:iron_sword || minecraft:stone_sword` would apply the attribute to both iron and stone swords, while `#minecraft:swords && !minecraft:stone_sword` would apply the attribute to all swords except stone swords.

**NEW** You can use the following components selectors to target items based on their components: `isFood`, `isEnchanted`, `hasDurability`, `isPotion`

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

`DEPENDENCY` sets the attribute value based on another attribute. It requires two additional fields: `dependency`, which is the attribute to base the value on, and `multiplier`, which is the value to multiply the dependency attribute by. For example, if you want to set the attack damage of an item to be double that item's attack speed, you would use:
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
`CONVERSION` transforms one attribute into another. It requires three additional fields: `from`, which is the attribute to convert from, `amount`, which is how much of the `from` attribute should be converted, and `rate`, which is the conversion rate. For example, if you want to remove all armor in the game and convert it into health, at a rate of 1 armor point = 2 health points, you would use:
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

**NEW**: `FOOD` is a special operation that only works with food items. It can configure all the aspects of a food item:
```json5
{
  "minecraft:golden_carrot": [
    {
      "operation": "FOOD",
      "remove": false, //whether to remove the food component from the item. If this is true, all other properties can be omitted. Optional, default is false.
      "nutrition": 6,
      "saturation": 14.4,
      "eat_seconds": 1.25, //time it takes to eat the food, in seconds. Optional, default is 1 second
      "can_always_eat": true, //whether the food can be eaten even if the player is not hungry. Optional, default is false
      "converts_to": "minecraft:carrot", //optional, item that the food converts to when eaten. Supports /give command format, so you can specify NBT data. For example, you can make the golden carrot give you a regular carrot with sharpness 5 like this: `minecraft:carrot{tag:{Enchantments:[{id:"minecraft:sharpness",lvl:5s}]}}`
      "effects": [ //optional, effects that are applied when the food is eaten
        {
          "effect": "minecraft:regeneration",
          "duration": 200, //duration in ticks, so 200 ticks = 10 seconds
          "amplifier": 1, //effect level - 1. So amplifier 0 is level 1, amplifier 1 is level 2, etc.
          "chance": 1.0 //chance for the effect to be applied, between 0 and 1
        }
      ],
    }
  ]
}
```

**NEW**: `FOOD_MODIFY` is a special operation that only works with food items. It acts as a multiplier and offset for the base attributes of the food.
```json5
{
  // This is going to make all golden foods in the game have double nutrition but take 0.5 seconds more to eat. it will also reduce the saturation by half and increase it by 5
  "isFood && regex:.*golden.*": [
    {
      "operation": "FOOD_MODIFY",
      "nutrition_multiplier": 2.0, //multiplier for the nutrition value. Optional, default is 1.0
      "nutrition_offset": 0, //offset for the nutrition value. Optional, default is 0
      "saturation_multiplier": 0.5, //multiplier for the saturation value. Optional, default is 1.0
      "saturation_offset": 5, //offset for the saturation value. Optional, default is 0
      "eat_seconds_multiplier": 1.0, //multiplier for the eat seconds value. Optional, default is 1.0
      "eat_seconds_offset": 0.5 //offset for the eat seconds value. Optional, default is 0
    }
  ]
}
```
### Slot
Can be mainhand, offhand, head, chest, legs, feet, or any Curios slot if prefixed with "curio:". Default value is mainhand

If the item's class extends ArmorItem(all armor items in the game do), the default value is based on the armor slot. This means that for most armors, you don't have to specify the slot.
