# Shortest Path integration handoff

## Objective

Add an optional integration with the RuneLite **Shortest Path** plugin. While the player is inside Tempoross, when a double harpoonfish fishing spot (`NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL`) appears, request a path from the player's current position to that NPC. Clear the requested path when either:

- the player reaches interaction range of the selected spot; or
- the selected spot despawns or changes into a different NPC.

This is a cross-plugin message integration. Do not add the Shortest Path repository or artifact as a dependency.

## Existing code relevant to the change

- `TemporossFirefighterPlugin` already tracks special fishing spot NPCs in the identity-backed `doubleFishingSpots` set.
- `updateTrackedNpc(NPC)` handles initial scene discovery, `NpcSpawned`, and `NpcChanged`.
- `onNpcDespawned(NpcDespawned)` removes despawned spots.
- `clearSceneTracking()` runs at encounter boundaries.
- `shutDown()` clears all plugin state.
- `TemporossStrategy` only consumes the boolean `TemporossState.isDoubleFishingSpotActive()` and must remain independent of RuneLite's event bus and concrete NPC instances.
- `TemporossInstruction` already has an optional `WorldPoint`, but this integration is spawn-driven rather than instruction-driven. Do not add the double-spot route to `TemporossStrategy`.

## Shortest Path message protocol

Inject RuneLite's `net.runelite.client.eventbus.EventBus` and post `net.runelite.client.events.PluginMessage` events.

Request a route:

```java
Map<String, Object> data = new HashMap<>();
data.put("start", client.getLocalPlayer().getWorldLocation());
data.put("target", selectedSpot.getWorldLocation());
eventBus.post(new PluginMessage("shortestpath", "path", data));
```

Clear the current route:

```java
eventBus.post(new PluginMessage("shortestpath", "clear"));
```

If Shortest Path is absent or disabled, the messages have no consumer and should fail harmlessly.

## Configuration

Add a config item to `TemporossFirefighterConfig` in the scene-highlights section. Suggested stable key and method:

```java
@ConfigItem(
	keyName = "pathToDoubleFishingSpot",
	name = "Path to double spot",
	description = "Uses the Shortest Path plugin to draw a route to a spawned double fishing spot",
	section = sceneHighlights,
	position = 8
)
default boolean pathToDoubleFishingSpot()
{
	return false;
}
```

Adjust later positions if necessary to keep them unique and ordered. This should be opt-in because it takes control of Shortest Path's single current route. It does not contact a third-party server, so the third-party-server warning in `AGENTS.md` is not applicable.

Subscribe to `ConfigChanged` in `TemporossFirefighterPlugin`. Check both the group and key. When this option becomes disabled, immediately clear a route owned by this plugin. When enabled during an encounter with an already tracked double spot, start a route to the selected spot rather than waiting for another spawn.

## State owned by `TemporossFirefighterPlugin`

Add state equivalent to:

```java
private NPC shortestPathDoubleSpot;
private boolean doubleSpotPathActive;
```

The NPC supplies both the current destination and its world area. A separate cached `WorldPoint` is optional. `doubleSpotPathActive` is important: only send `"clear"` if this plugin previously sent a `"path"` request that has not already been cleared. This reduces the chance of clearing a route subsequently created by the user or another plugin.

Do not repost the route every tick. Path requests should be driven by spawn/config events.

## Target selection

Create a small selector that chooses the closest valid NPC from `doubleFishingSpots` using the local player's world location. Identity-backed set iteration order is unspecified, so do not use `findFirst()` when multiple spots exist.

Selection rules:

1. Require the feature to be enabled.
2. Require `state.isInEncounter()`.
3. Require a non-null local player and player world location.
4. Ignore NPCs with a null world location/world area or a different plane.
5. Choose the NPC with the smallest world-area distance to the player.
6. If a route is already active for that exact NPC instance, do nothing.

When replacing one selected spot with another, post the new `"path"` request directly and update ownership state. A preliminary `"clear"` is unnecessary.

## Spawn and NPC-change behavior

Integrate with `updateTrackedNpc(NPC)`, but account for the fact that this method first removes the NPC from all tracking sets and is invoked for `NpcChanged` as well as spawn/discovery.

After classifying the NPC:

- If it is the special double fishing spot, add it to `doubleFishingSpots` and call the route-selection/update method.
- If it is the currently selected `shortestPathDoubleSpot` but its new ID is not the special spot ID, treat that as the selected destination disappearing: clear the owned path, then attempt to select another remaining special spot.

The initial `scanSceneOnce()` should also be able to initiate a route when the plugin starts or the encounter is detected after a spot has already spawned. Avoid duplicate requests by comparing NPC identity with the active selection.

## Despawn behavior

In `onNpcDespawned`:

1. Remove the NPC from the existing tracking sets as it does today.
2. If the NPC is the selected `shortestPathDoubleSpot`, clear the owned path.
3. Optionally select the nearest remaining double spot immediately. This is recommended because multiple valid special spots may coexist.
4. Preserve the existing state-boolean updates.

Removing an unrelated double spot must not clear the selected route.

## Arrival behavior

Check arrival once per `GameTick`; this is inexpensive and does not scan the scene. Do not require exact equality with `NPC.getWorldLocation()`, since NPCs occupy an area and the player generally fishes from an adjacent tile.

Recommended arrival condition:

```java
selectedSpot.getWorldArea().distanceTo(player.getWorldLocation()) <= 1
```

Also require the same plane. If the API version used by the project does not expose this exact overload, compute the equivalent distance from the NPC's world area rather than using exact tile equality.

On arrival, send `"clear"` once and set `doubleSpotPathActive` to false. Clear `shortestPathDoubleSpot` as well so that the still-present NPC does not retain stale route ownership. Do **not** immediately reselect it on subsequent ticks. Route selection must remain event-driven; otherwise arrival would cause the path to be recreated continuously.

This implies an arrival suppression rule: after reaching a spot, do not route to that same NPC again unless it despawns and a new special-spot NPC subsequently appears. The simplest implementation is a separate identity-backed `Set<NPC>` of reached/suppressed spots, cleared when those NPCs despawn and during scene cleanup. An alternative is to retain a `reachedDoubleSpot` identity field. Prefer the set if multiple special spots can coexist.

## Centralized cleanup

Create a helper such as `clearDoubleSpotPath()` which:

1. Posts `PluginMessage("shortestpath", "clear")` only if `doubleSpotPathActive` is true.
2. Clears `shortestPathDoubleSpot`.
3. Sets `doubleSpotPathActive` false.

Call it from:

- arrival;
- selected-NPC despawn/change;
- disabling the config item;
- `clearSceneTracking()`;
- `shutDown()`.

Clear any reached/suppressed NPC tracking in `clearSceneTracking()` and `shutDown()`. Cleanup must be idempotent, since encounter exit currently calls scene cleanup and shutdown may later run too.

## Important protocol limitation

Shortest Path exposes a global `"clear"` operation, not a route handle owned by the requesting plugin. It is impossible to prove that the visible route is still the one this plugin requested. Limit interference by:

- only clearing while `doubleSpotPathActive` is true;
- never posting repeated clears;
- clearing promptly upon arrival/despawn/config disable;
- documenting that manually replacing the path while a double-spot route is active may still result in that manual path being cleared by this plugin's later cleanup.

## Suggested method boundaries

Keep the integration localized in `TemporossFirefighterPlugin` with methods along these lines:

```text
updateDoubleSpotPathSelection()
startDoubleSpotPath(NPC spot)
checkDoubleSpotPathArrival()
clearDoubleSpotPath()
```

Do not add pathfinding behavior to overlays, `TemporossState`, or `TemporossStrategy`.

## Tests and verification

Add unit coverage where practical for selection/ownership logic. If concrete RuneLite NPC objects make this awkward, extract only the small deterministic selection/route-state decision into a package-private helper; do not over-engineer a new framework.

At minimum, run:

```powershell
./gradlew test
./gradlew build
```

Required in-game checks by the user:

1. With Shortest Path installed and the option enabled, enter Tempoross and verify a special spot spawn creates one route from the player's current location.
2. Walk adjacent to the selected NPC and verify the route clears once and does not immediately reappear.
3. Let the selected spot despawn before reaching it and verify the route clears.
4. If another special spot remains, verify the route transfers to the nearest remaining non-suppressed spot.
5. Disable the config while a route is active and verify immediate cleanup.
6. Leave the encounter while a route is active and verify cleanup.
7. Disable or uninstall Shortest Path and verify Tempoross Firefighter continues without errors.
8. Confirm normal double-spot highlighting and `TemporossStrategy` behavior remain unchanged.

Per `AGENTS.md`, a successful JVM start/build is not proof of correct behavior. Offer to launch the development client with `./gradlew run`, direct the user to the RuneLite wiki's **Using Jagex Accounts** instructions, and wait for their in-game confirmation.
