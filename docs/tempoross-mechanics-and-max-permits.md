# Tempoross Mechanics and Solo Cooking + Firefighting

This document summarizes the Tempoross encounter and the OSRS Wiki's **Solo
Cooking + Firefighting (Max Permits)** route.

Sources (reviewed 12 July 2026):

- [Tempoross](https://oldschool.runescape.wiki/w/Tempoross)
- [Tempoross strategies — Solo cooking + firefighting (max permits)](https://oldschool.runescape.wiki/w/Tempoross/Strategies#Solo_cooking_+_firefighting_(max_permits))

## Tempoross Firefighter route adjustments

The plugin intentionally starts its opening cooking cycle at **8 raw fish**
instead of the reference route's adjustable 9-fish baseline. It also uses
**91% storm intensity** as its loading cutoff, leaving extra travel time when
fires obstruct the route. The 9-fish and 93% values below describe the source
strategy rather than the plugin's guidance.

## Encounter objective

Tempoross is a non-combat Fishing boss requiring level 35 Fishing. A successful
encounter alternates between two phases:

1. **Surface phase:** catch harpoonfish, optionally cook them, and load them into
   a ship's ammunition crates. The cannons consume the loaded fish and reduce
   Tempoross' **Energy**.
2. **Submerged phase:** when Energy reaches 0%, fish from the spirit pool to
   reduce Tempoross' **Essence** while its Energy regenerates. If Essence reaches
   0 first, the encounter succeeds. If Energy returns to 100% first, Tempoross
   resurfaces and another surface phase begins.

Essence damage persists between submerged phases. After the third normal energy
drain, Tempoross becomes enraged and storm intensity rises very quickly; a route
that extends the encounter must have enough fish staged in both cannons to force
another submerge immediately.

## The three HUD resources

| Resource | Meaning | Failure or transition condition |
| --- | --- | --- |
| Energy | Tempoross' regenerating surface-phase defence | At 0%, Tempoross submerges and the spirit pool activates |
| Essence | The encounter's persistent boss health | At 0%, Tempoross is subdued |
| Storm intensity | The surface-phase time limit | At 100%, the encounter fails |

In a solo encounter, Tempoross has roughly 266 Energy. A raw harpoonfish removes
10 Energy and a cooked harpoonfish removes 15. The practical route thresholds
below are more useful than calculating from the displayed percentage because the
cannons fire over time.

## Actions and points

| Action | Points |
| --- | ---: |
| Catch a harpoonfish | 5 |
| Cook a harpoonfish | 10 |
| Load raw / cooked / crystallised fish | 20 / 65 / 20 |
| Repair a mast or totem pole | 40 |
| Douse a fire | 40 |
| Survive a wave while tethered | 10 |
| Scatter the spirit pool | 55 per successful scatter |

The first reward permit is earned at 2,000 points, with another permit for each
additional 700 points. The standard cooking and firefighting route is listed by
the wiki at approximately 14–18 permits per game (typically 15–16), or roughly
75–80 permits per hour.

## Hazards and arena objects

### Colossal wave

Use a rope to tether to a totem pole on the island or a mast on a ship. Full
Spirit angler counts as a rope. An untethered wave pushes the player and can
destroy encounter items. Waves can damage tether points, requiring a hammer to
repair them, and extinguish every active fire.

### Lightning and fire

Lightning clouds ignite 2x2 fires that spread if left alone. A bucket of water
douses one fire. Contact with fire can destroy ropes, hammers, buckets, and raw
or cooked harpoonfish. Losing fish during this inventory-tight route will often
invalidate its thresholds.

Fire is both a hazard and the main source of extra points in this method. The
safer approach is to douse island fires; the higher-scoring but riskier option is
to let some spread and douse them later. Never preserve a fire that blocks the
fishing-to-shrine-to-ship route or puts an inventory of fish at risk.

### Torrent

A torrent temporarily disables a cannon and stuns a player loading it. Moving
away or changing cannon avoids the stun, but switching is not inherently faster
than resuming the same cannon.

### Low-energy control

The route loads exactly 17 cooked fish first, leaving Tempoross at about 4%
Energy. This suppresses further special attacks and creates a controlled window
for fishing, cooking, and managing existing fires. Exact loading counts matter:
an extra cooked fish can submerge Tempoross too early and disrupt the route.

## Preparation

Keep **19 inventory slots free** for harpoonfish. Fill the other usable slots
with buckets of water.

Bring or equip:

- Full Spirit angler, or one rope if the full set is unavailable.
- An Imcando hammer in the off-hand, or one normal hammer.
- A rune pouch with the runes for **Humidify**.
- A steam/twinflame staff, or a tome of water/fire as appropriate for the rune
  setup.
- Preferably an infernal harpoon; its automatic cooking makes the tight fishing
  sections more consistent. A dragon harpoon is the next natural choice. A
  crystal harpoon complicates the 17/19 cooked-fish thresholds because
  crystallised fish do less Energy damage than cooked fish.

Humidify requires the Lunar spellbook. Full buckets are important: empty buckets
alone do not satisfy the firefighting route.

## Game strategy

This is the standard **Game strategy** from the linked Solo Cooking +
Firefighting section.

### Opening and first submerge

1. Run immediately to the nearest island fishing spot.
2. Catch **9 raw harpoonfish**, then begin cooking them at the shrine. Adjust the
   initial count slightly if the double spot consistently appears well before or
   after the initial fish are cooked.
3. Move to the double-fish spot as soon as it appears and reach **17 fish**.
4. If the colossal wave has not started, begin cooking. Tether when it starts;
   while already cooking, there is normally time to cook one or two more fish
   before tethering.
5. Cook all 17 fish and run to the ship. The storm should move in during this
   period, but it should be possible to reach the ship before fires spawn.
6. Load **exactly 17 cooked fish into the closest ammunition crate**. Tempoross
   should stop at about 4% Energy, preventing further special attacks.
7. Douse the ship fire.
8. Either douse the island fires for safety or leave some to spread and clear
   them only when they obstruct the route. The second option yields more points
   but increases the risk of a fire spreading onto the player while fishing or
   cooking.
9. Catch and cook **19 fish**. Load all 19 before storm intensity reaches 93%.
10. If intensity reaches 93% before the 19 are ready, immediately load as many
    cooked fish as possible. Fish the spirit pool for roughly 40% Essence damage,
    then return to finish fishing, cooking, and loading a total of 19. Tether if
    another colossal wave occurs.
11. Once all 19 have been loaded, fish the spirit pool until Tempoross regains
    its Energy and resurfaces. Doing no Essence damage in the first submerged
    phase is also recoverable if the extra time was required to prepare 19 fish.

### Second load and firefighting phase

1. Catch and cook another **19 fish** during the second surface phase.
2. If fire has spread significantly, collect 5–10 buckets and douse some fires
   while waiting for double-fish spots. Drop the excess buckets afterward to
   restore space for all 19 fish.
3. Before storm intensity reaches 93%, load all 19 cooked fish to force the
   second submerge.
4. Fish the spirit pool until Tempoross is below 60% Essence, preferably below
   50%.
5. Fill the inventory with buckets from the dock crate, cast Humidify while
   running to the nearest fire cluster, and douse fires while working around the
   island. Keep enough time in reserve to finish the encounter.

### Finish

1. Before storm intensity gets too high, catch **2 harpoonfish**. Cooking is
   optional if all useful fires are already out and time permits.
2. Load the 2 fish, taking particular care not to cross a fire with fish in the
   inventory.
3. Fish the spirit pool to finish Tempoross. Douse a few additional fires first
   only if the remaining Essence and storm intensity leave a comfortable margin.

## Safety rules and recovery

- **Fish are the priority.** Running through fire with only buckets is
  recoverable; running through it with harpoonfish can end the route.
- **Prioritize double spots**, but do not cross dangerous fire or miss the
  loading deadline to reach one.
- **93% storm intensity is the loading cutoff used by this route.** If the first
  19-fish load is incomplete at that point, load what is ready and use the
  documented partial spirit-pool recovery.
- **Clearing fires is the consistent option.** Letting them spread trades safety
  for more firefighting points and is not required by the standard route.
- **If fish are destroyed, salvage the game.** Abandon the max-permit thresholds
  and complete a conventional solo kill rather than risking all accumulated
  points.
- **Watch Essence before firefighting.** Reduce it below 60%, preferably below
  50%, during the second submerge so the final pool cycle can finish the fight.
