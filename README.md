# CustomEntityAttributes (CEA)

CEA Is a simple lightweight mod that can change the default attributes of any living entity through datapacks, inspired by [Jackiecrazy's Attributizer](https://github.com/Jackiecrazy/attributizer)


# How To Use

Inside your datapack folder, create a `customentityattributes` folder, and inside it you can put as many json files as you want, with this format:

```json
{
  "minecraft:creeper": [
    {
	    "attribute": "minecraft:generic.max_health",
	    "uuid": "0e1c07ef-d456-4567-b748-96b6f84b409e", //optional
	    "value": 5,
	    "operation": "BASE"
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
  ]
}
```
### Object Key
Which entity ID will be changed. If a the first character is a # the key is treated as a tag. In the example above, all entities tagged as raiders will have +8 health, and all creepers will have 5 health.

### Attribute
Which attribute should be changed, supports modded attributes.

### Operation
Can be `ADDITION`, `MULTIPLY_BASE`, `MULTIPLY_TOTAL`, and `BASE`. The first tree are explained in the [MC Wiki](https://minecraft.fandom.com/wiki/Attribute#Operations), and BASE means it will override the default base value for that attribute
