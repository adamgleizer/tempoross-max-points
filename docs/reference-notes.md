# Tempoross Solo Helper Reference Notes

Source specimen: [RyanTamulevicz/tempoross-solo-helper](https://github.com/RyanTamulevicz/tempoross-solo-helper)

These notes capture useful facts and patterns observed in the reference plugin. They are research notes, not a design specification for Tempoross Firefighter.

## Useful RuneLite constants

The reference uses `net.runelite.api.gameval` constants rather than embedding numeric IDs in code.

### Items

| Purpose | Gameval constant | Current value when inspected |
| --- | --- | ---: |
| Raw harpoonfish | `ItemID.TEMPOROSS_RAW_HARPOONFISH` | 25564 |
| Cooked harpoonfish | `ItemID.TEMPOROSS_HARPOONFISH` | 25565 |
| Crystallised harpoonfish | `ItemID.TEMPOROSS_CRYSTALLISED_HARPOONFISH` | 25566 |
| Empty bucket | `ItemID.BUCKET_EMPTY` | 1925 |
| Bucket of water | `ItemID.BUCKET_WATER` | 1929 |
| Rope | `ItemID.ROPE` | 954 |
| Basic harpoon | `ItemID.HARPOON` | 311 |

The named constants should remain the source of truth because numeric values can change.

### NPCs and objects

- Normal harpoonfish spots: `NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH` (`10565`) and `..._SOUTH` (`10568`).
- Double-fish spot: `NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL` (`10569`).
- Spirit pool: `NpcID.TEMPOROSS_P2_FISHINGSPOT_INACTIVE` (`10570`) and `NpcID.TEMPOROSS_P2_FISHINGSPOT` (`10571`).
- Ammunition hoppers: `NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1` through `_4` (`10576`-`10579`). Despite the name, these are represented as NPCs.
- Active fire hitbox: `NpcID.TEMPOROSS_FIRE_HITBOX` (`8643`).
- Fire visual fallback: `ObjectID.TEMPOROSS_FIRE_VISUALS`.
- Victory Spirit Anglers: `NpcID.TEMPOROSS_INSTANCE_HOST_E_VICTORY`, `_W_VICTORY`, `_N_VICTORY`, and `_S_VICTORY` (`10586`, `10588`, `10595`, and `10597`).

### Tempoross HUD

Useful components are grouped under `InterfaceID.TemporossHud`, including:

- `UNIVERSE` and `STATUS`
- `ENERGY` and the energy bar components
- `ESSENCE` and the essence bar components
- `STORM_INTENSITY` and the storm bar components
- `POINTS_TEXT`

The reference detects an encounter when selected HUD components are visible. It reads essence from percentage text first, then falls back to the ratio between the foreground and background bar widths.

No useful Tempoross varbit, varplayer, or explicit region/area check was used by the reference.

## Event and overlay patterns

- Inventory is sampled from `InventoryID.INV` on `GameTick`.
- Relevant NPCs and objects are maintained with spawn, change, and despawn events.
- A one-time scene scan populates tracking collections when the encounter is first detected. It does not scan the entire scene every tick.
- The special double-fish spot is preferred over the nearest normal fishing spot.
- Targets are rendered using a clickbox or convex hull, with a canvas tile polygon as a fallback, plus a canvas text label.
- The observed loading chat text contains `you start loading harpoonfish into the cannon` after case normalization.
- The reference infers the player's working shore from the hopper they interact with and evidence that fish were successfully loaded.
- Spirit-pool appearance followed by disappearance is used as a phase-transition signal.

## Route observations

The specimen implements one no-cook solo route: an opening trip of 25 or 26 fish, later 27-fish trips, a three-fish partial load, an essence target near 10%, and a final kill phase. These thresholds describe that particular strategy and should not be treated as universal Tempoross rules.

## Cautions for Tempoross Firefighter

- Keep collection, encounter state, strategy decisions, and rendering in separate classes; the reference plugin class combines too many responsibilities.
- Treat chat strings and recursive widget-text parsing as fallbacks because wording and widget structure are brittle.
- Treat bar-width percentage calculations as estimates and guard against hidden or zero-width widgets.
- Avoid depending on exact inventory transitions alone; dropped fish, missed events, cooking, and unusual player routes can confuse automatic progression.
- The working-shore calculation assumes four known hopper locations and a stable arena layout.
- The reference's manual overlay menu entries and hotkeys are intentionally not part of the initial Tempoross Firefighter MVP.
- Retain event-backed scene tracking and keep overlay rendering inexpensive.

Tempoross Firefighter must remain guidance-only: no injected input, menu manipulation, clicks, movement, or game actions.

## Additional legacy specimen

Additional source: [nmlynch94/runelite-external-plugins, `tempoross` branch](https://github.com/nmlynch94/runelite-external-plugins/tree/tempoross)

`src/main/java/com/tempoross/TemporossPlugin.java` contains several useful historical observations:

- Tempoross instance region `12076`, Unkah reward-pool region `12588`, and Unkah boat region `12332`.
- Varbit `11895` was treated as a tether bitfield, while `11936` held the reward-pool permit count.
- Double-spot NPC `10569` was detected from `NpcSpawned`.
- Wave chat strings included `a colossal wave closes in...`, `as the wave washes over you`, and `the wave slams into you`.
- `tempoross is vulnerable` was used as a phase-change chat signal.
- Inventory changes were used to count raw, cooked, and crystallised harpoonfish.

This plugin also relies on raw object IDs, script-stack positions, wall-clock fire/wave timers, and predictive notifications. Those techniques are old, brittle, and are not suitable for Tempoross Firefighter. In particular, predictive boss-mechanic timers and warnings conflict with the current project restrictions. Named gameval constants, visible current state, and non-predictive guidance should be preferred.
