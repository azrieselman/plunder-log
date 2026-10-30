# Plunder Log

A small NeoForge mod for Minecraft 1.21.1. It remembers when a vanilla structure has been cleared, per world, and lists those clears in a journal.

The server decides. Every player in that world shares the same log, and it is still there after a restart.

## What "cleared" means

A structure is one generated structure start (a desert pyramid, a dungeon, a mansion), identified by its structure id and the position of that start.

It is marked cleared when either of these happens, depending on config:

- **Chests.** Every loot chest, barrel, and shulker box that sits in a structure piece (or within the configured range of one) is empty, and every chunk those pieces occupy is loaded. Unopened loot still counts as full. Close the container after you empty it; that is when the check runs.
- **Boss.** A mob in the `plunderlog:structure_bosses` entity tag dies inside a structure piece. The default tag is the elder guardian, the warden, and the evoker. One death is enough. Add more mobs with a datapack; the mod does not special-case other mods' bosses.

The smallest structure piece set that contains the chest or the death is the one that gets the mark, so a mineshaft under a pyramid does not steal the clear.

## Journal

Craft a book and a compass into a Plunder Journal, or take one from the Tools & Utilities creative tab. Use it to open the list: structure name, dimension, position, and when it was cleared (real time and world day).

## Config

`config/plunderlog-common.toml` on the server (or in the instance, for singleplayer):

- `clearMode`: `CHESTS`, `BOSSES`, or `BOTH` (default `BOTH`)
- `chestRange`: how many blocks outside a structure piece a loot container still counts (default `4`)

There is also a config screen on the Mods list.

## Xaero's Minimap and World Map

If Xaero's Minimap is installed, each cleared structure is added as a third-party waypoint through the same API Minimap 26.x uses for Waystones. The same id is replaced rather than duplicated. Marks are refreshed when you join and when you press **Add Map Marks** in the journal.

Xaero's World Map shows those marks when Minimap is installed next to it. World Map by itself does not expose that API, so no waypoint is added in that case. The journal still works. WITS is not used.

## Build

Java 21.

```bash
./gradlew build
```

The mod jar is `build/libs/plunder-log-1.0.0.jar`.

The Gradle setup comes from the NeoForge 1.21.1 ModDevGradle template. See `TEMPLATE_LICENSE.txt`.

## Limits

- Only loaded structure-piece chunks are checked. A stronghold or mineshaft stays uncleared until its pieces are loaded and every loot container in them is empty.
- Vaults, hoppers, and crafters are not loot. Trial-chamber vaults do not count.
- A chest, barrel, or shulker box you place inside a structure piece is treated like the structure's own. Items left in it block a chest clear.
- Jungle-temple dispensers count while they still have a loot table or items.
- Bosses only count when they die inside a piece. The default tag is vanilla structure bosses, not a hardcoded list of mod ids.
