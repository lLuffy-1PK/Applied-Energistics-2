# Loot Tables
***
AE2 adds a few loot tables for generating its chest loot. Players or modpack authors may want to configure these loot
tables. Explained below is the format of the loot tables.

## Mineshaft loot

In loot table: `minecraft:chests/abandoned_mineshaft`

Names of loot pools added to the table by this mod:
- `AE2 Crystals`
- `AE2 DUSTS`

The loot pools added for certus quartz crystals and dust that can spawn in mineshaft chests. These are added
programmatically, so they are not overridable by JSON.  

If you are a modpack author that needs to override these, consider using something like LootTweaker.

## Meteorite loot
In custom loot table: `appliedenergistics2:meteor_loot`

Names of loot pools added to the table by this mod:
- `presses`
- `nuggets`  

This determines what loot will be spawned in the center of meteorites. This loot table
**can** be overrided by JSON. The default loot table defined 
[here](/src/main/resources/assets/appliedenergistics2/loot_tables/meteor_loot.json) (`appliedenergistics2:meteor_loot`)
uses custom conditions and functions in order to account for enabled/disabled items in the config and to support ore
dictionaries.

### Custom conditions
`appliedenergistics2:loot_can_roll` - This condition only allows the entry to be rolled `max` number of times,
regardless of the number of `rolls` specified by the pool. To be used in tandem with the custom `tally_loot` function.   
Example:
```yaml
{
    "condition": "appliedenergistics2:loot_can_roll",    # The name of this condition
    "id": "material.press.process.calculation",          # The id of the item to check. This must match the id of a corresponding tally_loot function.
    "max": 1,                                            # The max number of times this entry can be rolled.
    "context_id": 0                                      # Optional. A unique integer if you want to tally the same id in a different entry.
}
```
Notes:
- `context_id` defaults to 0 and is optional. The only use case for it is if you have a separate entry that has the same
`id` as this entry, and you want a separate count of that entry.

### Custom functions
(1) `appliedenergistics2:transform_to_maybe_item` - This function transforms the input item into 
an AE2 item that could be absent.  
Example:
```yaml
{
  "function": "appliedenergistics2:transform_to_maybe_item",    # The name of this function
  "id": "material.press.processor.calculation"                  # The id of the item to transform into.
}
```
Notes:
- Currently, only the following values for `id` are supported.
  - `material.press.processor.calculation`: calculation press
  - `material.press.processor.engineering`: engineering press
  - `material.press.processor.logic`: logic press
  - `material.press.silicon`: silicon press
  - `sky_stone_block`: sky stone
- This only exists to account for disabled items in the config. If you are overriding this JSON, you likely do not need
to use this function as you control the exact mod configs and the mod's environment. Just use the item id directly in
the entry's `name`, like usual.

(2) `appliedenergistics2:transform_to_random_ore` - This function transforms the input item into
a random OreDictionary item specified.  
Example:
```yaml
{
  "function": "appliedenergistics2:transform_to_random_ore",    # The name of this function
  "ores": [                                                     # The list of ore dictionary values to choose from randomly.
    "nuggetIron",                                               # Transform into this oredict item...
    "nuggetGold"                                                # or this oredict item...
    # or any other ones you specify here.
  ]
}
```

(3) `appliedenergistics2:tally_loot` - This function increments the number of times this entry has been rolled. To
be used for tracking with the custom `loot_can_roll` condition.  
Example:
```yaml
{
  "function": "appliedenergistics2:tally_loot",    # The name of this function
  "id": "material.press.process.calculation",      # The id of the item to check. This must match the id of a corresponding loot_can_roll condition.               
  "context_id": 0                                  # Optional. A unique integer if you want to tally the same id in a different entry.
}
```
Notes:
- `context_id` defaults to 0 and is optional. The only use case for it is if you have a separate entry that has the same `id` as this entry, and you want a separate count of that entry.