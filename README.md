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

In the example above, all creepers will have 5 health, and +10 follow range. All entities tagged as raiders will have +8 health. The mob "example:anymob" will have 10 projectile damage.
### Object Key
Which entity ID will be changed. If the first character is a # the key is treated as a tag. In the example above, all entities tagged as raiders will have +8 health, and all creepers will have 5 health. If no namespace is provided, it uses the filename. For example, if I'm in file "alexsmobs.json", and I'm editing entity "void_worm", instead of typing "alexsmobs:voidworm", I can just type "voidworm"

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
Which item ID will be changed. If the first character is a # the key is treated as a tag. If no namespace is provided, it uses the filename. For example, if I'm in file "alexsmobs.json", and I'm editing item "emu_leggings", instead of typing "alexsmobs:emu_leggings", I can just type "emu_leggings"

### UUID
This is how minecraft knows which item has which modifier. ~~If you only have one modifier in the item you can ignore this, but if you have more than one, you should generate a UUID for each one. You can use [this site](https://www.uuidgenerator.net/) to generate one. BASE operation doesn't need a UUID~~. This is no longer necessary as of update 1.11

### Attribute
Which attribute should be changed, supports modded attributes.

### Operation
Can be `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL` and `BASE`. They are all in the [MC Wiki](https://minecraft.fandom.com/wiki/Attribute#Operations) except `BASE`, that removes all other modifiers for that attribute and sets the value to the one in the json file.

### Slot
Can be mainhand, offhand, head, chest, legs, feet, or any Curios slot if prefixed with "curio:". Default value is mainhand, if the item's class extends ArmorItem, the default value is based on the armor slot. This means that for most armors, you don't have to specify the slot. You still have to specify the Curios slot.