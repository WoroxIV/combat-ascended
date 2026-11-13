# Combat Ascended

A combat system for **any** weapon mod: combos, parries, dashes, dual wielding, two-handed weapons, area finishers, on-hit effects and armor penetration, with about 50 weapon archetypes, an in-game editor and animated tutorials.

- **Mod ID:** `combat_ascended`
- **Loaders:** NeoForge and Fabric on Minecraft 1.21.1, Forge and Fabric on 1.20.1
- **Works with:** Better Combat, Better Weaponry, Simply Swords and any mod that adds weapons
- **Not compatible with:** Epic Fight. The game refuses to start with both, because both replace the whole combat system.

## How a weapon gets its behavior

Combat Ascended looks for a behavior for every item, in this order:

1. A weapon file that lists the item.
2. A tag `combat_ascended:archetypes/<archetype>`.
3. A Better Combat `weapon_attributes` file or an Epic Fight `capabilities/weapons` file shipped by the mod, even when those mods are not installed.
4. The words of the item's name (`iron_longsword` becomes `longsword`), when the item is really a weapon.
5. Any other weapon behaves like a vanilla sword: it can parry.

An item counts as a weapon if it is a sword, axe or trident class, has a weapon tag, or gives attack damage in the main hand. Detected weapons never lock the off hand, and keep their own right click if they have one. The six vanilla swords can parry and do nothing else by default.

Run `/combatascended scan` to list every weapon, its archetype and the reason. The report is written to `config/combat_ascended/scan.json`.

## Adding weapons from your mod or modpack

A weapon takes three lines. Put this file in your mod or in a data pack, at `data/<mod>/combat_ascended/weapons/<name>.json`:

```json
{ "parent": "combat_ascended:katana", "items": ["mymod:frost_katana", "#mymod:katanas"] }
```

Write only what you change. Objects merge field by field, and lists are replaced:

```json
{
  "parent": "combat_ascended:katana",
  "items": ["mymod:storm_katana"],
  "reach_bonus": 1.0,
  "on_hit_effects": [{ "effect": "minecraft:slowness", "duration_seconds": 2, "level": 1, "chance": 100 }],
  "combo": { "finisher": { "radius": 5, "angle": 120, "damage_multiplier": 1.6, "cooldown_multiplier": 1.6, "gesture": "slash" } }
}
```

`combo.finisher` changes only the last hit, so you do not have to rewrite the whole list.

- **New archetype:** `data/<mod>/combat_ascended/archetypes/<name>.json`, same fields.
- **New words for the detection:** `data/<mod>/combat_ascended/detection/<name>.json`, or `config/combat_ascended/detection.json`:

```json
{ "keywords": [{ "words": ["nagamaki"], "archetype": "combat_ascended:naginata" }], "excluded_words": ["sawblade"] }
```

Files in `config/combat_ascended/weapons/<mod>/<name>.json` and in world data packs override the mods. `weapon.schema.json` in the config folder gives auto-completion in VS Code.

### Java API

`com.combatascended.api.CombatAscendedApi` gives the profile of an item and lets you listen to parries and combos. Everything else can change between versions.

## Archetypes

`sword`, `shortsword`, `longsword`, `broadsword`, `cutlass`, `scimitar`, `saber`, `machete`, `rapier`, `estoc`, `greatsword`, `claymore`, `katana`, `tachi`, `nodachi`, `dagger`, `knife`, `kukri`, `sai`, `claw`, `fist`, `tonfa`, `sickle`, `kama`, `scythe`, `axe`, `battleaxe`, `greataxe`, `double_axe`, `mace`, `club`, `hammer`, `warhammer`, `greathammer`, `maul`, `flail`, `morningstar`, `spear`, `pike`, `halberd`, `glaive`, `naginata`, `lance`, `trident`, `javelin`, `quarterstaff`, `battlestaff`, `twinblade`, `warglaive`, `chakram`, `whip`, `anchor`.

## Fields of a weapon or archetype file

| Field | Meaning |
|---|---|
| `parent` | Archetype this file starts from |
| `items` | Items or `#tags` that get this behavior |
| `two_handed` / `dual_wielded` | Locks the off hand / can be used in pairs |
| `reach_bonus` | Blocks added to the item's reach |
| `exact_reach` | The displayed reach is exact, even in Creative mode |
| `armor_penetration` | % of the target's armor ignored (with Apothic Attributes, its Armor Shred is used) |
| `disables_shield` | Disables a raised shield, like an axe |
| `on_hit_effects` | Effects given to the target |
| `sweep` | `enabled`, `radius`, `damage_ratio` |
| `ability` | `none`, `parry` or `dash`, with `parry` and `dash` objects for their settings |
| `combo` | `enabled`, `reset_after_seconds`, `min_charge`, `max_bonus_damage`, `steps`, `finisher` |
| `combo.steps[]` | `both_hands`, `radius`, `angle`, `damage_multiplier`, `knockback`, `cooldown_multiplier`, `gesture` (`slash`, `thrust`, `overhead`), `sound`, `particles` |

`rules.json` turns each system on or off. `gear.json` holds the shield parry settings and how often mobs raise their guard.

## With Better Combat

Better Combat handles reach, sweep, two-handed weapons, dual wielding and the attack animations. Combat Ascended keeps combos, parries, dashes, on-hit effects, enchantments, shield disabling, armor penetration, mob behaviors and the PvP cap. Each swing counts once in a combo, even when it hits several targets.

## Coming from Better Weaponry 2.0

The combat system of Better Weaponry 2.0 moved here. At first launch, Combat Ascended imports `rules.json`, the combat fields of your weapon files, the shield settings and your display preferences. The original files are not changed. Riposte, Reflective and Momentum on existing items keep working.

## Development

```powershell
./gradlew build                 # build the jars
./gradlew runGameTestServer     # run the automated tests on every loader
./gradlew :neoforge:runData     # regenerate the translations
```

The tests use the real weapons of Better Weaponry, built in the sibling `BetterWeaponry-<version>` folder. Nothing from Better Weaponry goes into the published jar.

## License

All Rights Reserved, © WoroxIV. See [LICENSE](LICENSE): you may use this mod in any modpack, and any mod may integrate with it through its data files and API.
