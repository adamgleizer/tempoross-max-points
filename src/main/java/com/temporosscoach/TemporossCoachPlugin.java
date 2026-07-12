package com.temporosscoach;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameObject;
import net.runelite.api.ItemContainer;
import net.runelite.api.Item;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

@Slf4j
@PluginDescriptor(
	name = "Tempoross Coach",
	description = "Guidance and inventory tracking for Tempoross",
	tags = {"tempoross", "fishing", "coach", "helper"}
)
public class TemporossCoachPlugin extends Plugin
{
	private static final String WAVE_INCOMING_MESSAGE = "a colossal wave closes in...";
	private static final String WAVE_END_SAFE_MESSAGE = "as the wave washes over you";
	private static final String WAVE_END_DANGEROUS_MESSAGE = "the wave slams into you";
	static final int WAVE_IMPACT_MILLIS = 7_800;
	private static final int FIRE_SPREAD_MILLIS = 24_000;
	private static final int FIRE_SPAWN_MILLIS = 9_600;
	private static final int FIRE_SPREADING_SPAWN_MILLIS = 1_200;
	private static final WorldPoint NORTH_AMMUNITION_CRATE_INTERACTION_TILE =
		new WorldPoint(3037, 2850, 0);

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private TemporossCoachConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TemporossStepOverlay stepOverlay;

	@Inject
	private TemporossPreparationOverlay preparationOverlay;

	@Inject
	private TemporossObjectOverlay objectOverlay;

	@Inject
	private HumidifySpellOverlay humidifySpellOverlay;

	@Inject
	private TemporossPathOverlay pathOverlay;

	@Inject
	private TemporossHudReader hudReader;

	private final TemporossState state = new TemporossState();
	private final TemporossStrategy strategy = new TemporossStrategy();
	private final Set<NPC> activeSpiritPools = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> fishingSpots = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> doubleFishingSpots = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Object> reachedPathTargets = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> ammunitionCrates = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<GameObject> cookingShrines = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<GameObject, TemporossTimedObject> fireObjects = new IdentityHashMap<>();
	private final Set<GameObject> tetherPoints = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<GameObject> ropeSupplies = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<GameObject> hammerSupplies = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<GameObject> bucketSupplies = Collections.newSetFromMap(new IdentityHashMap<>());
	private boolean waveIncoming;
	private boolean wasInEncounter;
	private Instant waveImpactTime;
	private WaterBucketInfoBox waterBucketInfoBox;
	private HumidifyInfoBox humidifyInfoBox;
	private boolean waterBucketInfoBoxVisible;
	private boolean humidifyInfoBoxVisible;
	private Object guidancePathTarget;
	private WorldPoint guidancePathDestination;
	private List<WorldPoint> guidancePath = Collections.emptyList();
	private PathTargetKind completedPathKind;
	private PathTargetKind activePathKind;
	private GameObject waveTetherTarget;
	private TemporossInstruction instruction = TemporossStrategy.placeholderInstruction();

	@Provides
	TemporossCoachConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TemporossCoachConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateTimerMode("highlightFires", "fireTimerMode");
		migrateTimerMode("useWaveTimer", "waveTimerMode");
		overlayManager.add(preparationOverlay);
		overlayManager.add(stepOverlay);
		overlayManager.add(objectOverlay);
		overlayManager.add(humidifySpellOverlay);
		overlayManager.add(pathOverlay);
		preparationOverlay.applyDefaultSize();
		stepOverlay.applyDefaultSize();
		waterBucketInfoBox = new WaterBucketInfoBox(
			itemManager.getImage(ItemID.BUCKET_WATER), this, state);
		humidifyInfoBox = new HumidifyInfoBox(null, this);
		spriteManager.getSpriteAsync(SpriteID.LunarMagicOn.HUMIDIFY, 0, humidifyInfoBox);
		updateInfoBoxes();
		log.debug("Tempoross Coach started");
	}

	@Override
	protected void shutDown()
	{
		clearGuidancePath();
		reachedPathTargets.clear();
		overlayManager.remove(preparationOverlay);
		overlayManager.remove(stepOverlay);
		overlayManager.remove(objectOverlay);
		overlayManager.remove(humidifySpellOverlay);
		overlayManager.remove(pathOverlay);
		infoBoxManager.removeInfoBox(waterBucketInfoBox);
		waterBucketInfoBox = null;
		waterBucketInfoBoxVisible = false;
		infoBoxManager.removeInfoBox(humidifyInfoBox);
		humidifyInfoBox = null;
		humidifyInfoBoxVisible = false;
		hudReader.reset();
		activeSpiritPools.clear();
		fishingSpots.clear();
		doubleFishingSpots.clear();
		ammunitionCrates.clear();
		cookingShrines.clear();
		fireObjects.clear();
		tetherPoints.clear();
		ropeSupplies.clear();
		hammerSupplies.clear();
		bucketSupplies.clear();
		waveIncoming = false;
		wasInEncounter = false;
		waveImpactTime = null;
		state.clear();
		strategy.reset();
		instruction = TemporossStrategy.placeholderInstruction();
		log.debug("Tempoross Coach stopped");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		state.updateInventory(inventory);
		state.setHarpoonPresent(
			containsHarpoon(inventory)
				|| weaponIsHarpoon(client.getItemContainer(InventoryID.WORN)));
		updateRunePouchContents();
		updateInfoBoxes();
		hudReader.update(state);
		if (state.isInEncounter() && !wasInEncounter)
		{
			clearSceneTracking();
			scanSceneOnce();
		}
		else if (!state.isInEncounter() && wasInEncounter)
		{
			clearSceneTracking();
		}
		if (!state.isInEncounter())
		{
			activeSpiritPools.clear();
			waveIncoming = false;
			waveImpactTime = null;
		}
		state.setSpiritPoolActive(!activeSpiritPools.isEmpty());
		state.setDoubleFishingSpotActive(!doubleFishingSpots.isEmpty());
		state.setTethered(client.getVarbitValue(VarbitID.TEMPOROSS_TETHERED) > 0);
		pruneStaleTetherPoints();
		instruction = strategy.getInstruction(state);
		updateWaveTetherTarget();
		updateGuidancePath();
		wasInEncounter = state.isInEncounter();
	}

	private void updateRunePouchContents()
	{
		if (state.getRunePouches() < 1)
		{
			return;
		}

		int[] runeVarbits = {
			VarbitID.RUNE_POUCH_TYPE_1,
			VarbitID.RUNE_POUCH_TYPE_2,
			VarbitID.RUNE_POUCH_TYPE_3,
			VarbitID.RUNE_POUCH_TYPE_4,
			VarbitID.RUNE_POUCH_TYPE_5,
			VarbitID.RUNE_POUCH_TYPE_6
		};
		int[] quantityVarbits = {
			VarbitID.RUNE_POUCH_QUANTITY_1,
			VarbitID.RUNE_POUCH_QUANTITY_2,
			VarbitID.RUNE_POUCH_QUANTITY_3,
			VarbitID.RUNE_POUCH_QUANTITY_4,
			VarbitID.RUNE_POUCH_QUANTITY_5,
			VarbitID.RUNE_POUCH_QUANTITY_6
		};
		EnumComposition runePouchRunes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		int astral = 0;
		int water = 0;
		int fire = 0;
		for (int slot = 0; slot < runeVarbits.length; slot++)
		{
			int quantity = client.getVarbitValue(quantityVarbits[slot]);
			int runeType = client.getVarbitValue(runeVarbits[slot]);
			if (quantity <= 0 || runeType == 0)
			{
				continue;
			}

			int runeItemId = runePouchRunes.getIntValue(runeType);
			if (runeItemId == ItemID.ASTRALRUNE)
			{
				astral += quantity;
			}
			else if (runeItemId == ItemID.WATERRUNE)
			{
				water += quantity;
			}
			else if (runeItemId == ItemID.FIRERUNE)
			{
				fire += quantity;
			}
		}
		state.addRunePouchContents(astral, water, fire);
	}

	private boolean containsHarpoon(ItemContainer itemContainer)
	{
		if (itemContainer == null)
		{
			return false;
		}

		for (Item item : itemContainer.getItems())
		{
			if (item != null && item.getId() >= 0 && item.getQuantity() > 0 && isHarpoon(item))
			{
				return true;
			}
		}
		return false;
	}

	private boolean weaponIsHarpoon(ItemContainer equipment)
	{
		if (equipment == null)
		{
			return false;
		}

		Item[] items = equipment.getItems();
		int weaponSlot = EquipmentInventorySlot.WEAPON.getSlotIdx();
		return weaponSlot < items.length
			&& items[weaponSlot] != null
			&& items[weaponSlot].getId() >= 0
			&& items[weaponSlot].getQuantity() > 0
			&& isHarpoon(items[weaponSlot]);
	}

	private boolean isHarpoon(Item item)
	{
		String name = itemManager.getItemComposition(item.getId()).getName();
		return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("harpoon");
	}

	private void updateInfoBoxes()
	{
		if (waterBucketInfoBox != null)
		{
			boolean shouldShowWaterBuckets = config.showWaterBucketInfoBox();
			if (shouldShowWaterBuckets && !waterBucketInfoBoxVisible)
			{
				infoBoxManager.addInfoBox(waterBucketInfoBox);
				waterBucketInfoBoxVisible = true;
			}
			else if (!shouldShowWaterBuckets && waterBucketInfoBoxVisible)
			{
				infoBoxManager.removeInfoBox(waterBucketInfoBox);
				waterBucketInfoBoxVisible = false;
			}
		}

		if (humidifyInfoBox == null)
		{
			return;
		}

		boolean shouldShow = config.showHumidifyInfoBox()
			&& state.getWaterBuckets() <= 2;
		if (shouldShow && !humidifyInfoBoxVisible)
		{
			infoBoxManager.addInfoBox(humidifyInfoBox);
			humidifyInfoBoxVisible = true;
		}
		else if (!shouldShow && humidifyInfoBoxVisible)
		{
			infoBoxManager.removeInfoBox(humidifyInfoBox);
			humidifyInfoBoxVisible = false;
		}
	}

	private void scanSceneOnce()
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}

		for (Tile[][] plane : worldView.getScene().getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}
					for (GameObject gameObject : tile.getGameObjects())
					{
						if (gameObject != null)
						{
							updateTrackedGameObject(gameObject);
						}
					}
				}
			}
		}

		for (NPC npc : worldView.npcs())
		{
			updateTrackedNpc(npc);
		}
	}

	private void clearSceneTracking()
	{
		clearGuidancePath();
		reachedPathTargets.clear();
		activeSpiritPools.clear();
		fishingSpots.clear();
		doubleFishingSpots.clear();
		ammunitionCrates.clear();
		cookingShrines.clear();
		fireObjects.clear();
		tetherPoints.clear();
		ropeSupplies.clear();
		hammerSupplies.clear();
		bucketSupplies.clear();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!state.isInEncounter() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage().toLowerCase();
		if (message.contains(WAVE_INCOMING_MESSAGE))
		{
			if (!waveIncoming)
			{
				if (config.waveNotification().isEnabled())
				{
					notifier.notify(config.waveNotification(), "A colossal wave closes in...");
				}
				waveImpactTime = Instant.now().plusMillis(WAVE_IMPACT_MILLIS);
			}
			waveIncoming = true;
		}
		else if (message.contains(WAVE_END_SAFE_MESSAGE)
			|| message.contains(WAVE_END_DANGEROUS_MESSAGE))
		{
			waveIncoming = false;
			waveImpactTime = null;
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		updateTrackedNpc(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		updateTrackedNpc(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		activeSpiritPools.remove(npc);
		fishingSpots.remove(npc);
		doubleFishingSpots.remove(npc);
		reachedPathTargets.remove(npc);
		if (npc == guidancePathTarget)
		{
			clearGuidancePath();
		}
		ammunitionCrates.remove(npc);
		state.setSpiritPoolActive(!activeSpiritPools.isEmpty());
		state.setDoubleFishingSpotActive(!doubleFishingSpots.isEmpty());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		updateTrackedGameObject(event.getGameObject());
	}

	private void updateTrackedGameObject(GameObject gameObject)
	{
		cookingShrines.remove(gameObject);
		fireObjects.remove(gameObject);
		tetherPoints.remove(gameObject);
		ropeSupplies.remove(gameObject);
		hammerSupplies.remove(gameObject);
		bucketSupplies.remove(gameObject);
		reachedPathTargets.remove(gameObject);
		if (gameObject == guidancePathTarget)
		{
			clearGuidancePath();
		}
		if (gameObject == waveTetherTarget)
		{
			waveTetherTarget = null;
		}

		if (gameObject.getId() == ObjectID.TEMPOROSS_SHRINE_FIRE)
		{
			cookingShrines.add(gameObject);
		}
		if (isFireObject(gameObject.getId()))
		{
			fireObjects.put(gameObject,
				new TemporossTimedObject(Instant.now(), fireDuration(gameObject.getId())));
		}
		if (isTetherPoint(gameObject.getId()))
		{
			tetherPoints.add(gameObject);
		}
		if (gameObject.getId() == ObjectID.TEMPOROSS_CRATE_ROPE)
		{
			ropeSupplies.add(gameObject);
		}
		else if (gameObject.getId() == ObjectID.TEMPOROSS_CRATE_HAMMER)
		{
			hammerSupplies.add(gameObject);
		}
		else if (gameObject.getId() == ObjectID.TEMPOROSS_CRATE_BUCKET)
		{
			bucketSupplies.add(gameObject);
		}
	}

	private void pruneStaleTetherPoints()
	{
		tetherPoints.removeIf(gameObject -> !isPresentOnSceneTile(gameObject));
	}

	private static boolean isPresentOnSceneTile(GameObject gameObject)
	{
		Point sceneLocation = gameObject.getSceneMinLocation();
		Tile[][][] tiles = gameObject.getWorldView().getScene().getTiles();
		int plane = gameObject.getPlane();
		int sceneX = sceneLocation.getX();
		int sceneY = sceneLocation.getY();
		if (plane < 0 || plane >= tiles.length
			|| sceneX < 0 || sceneX >= tiles[plane].length
			|| sceneY < 0 || sceneY >= tiles[plane][sceneX].length)
		{
			return false;
		}

		Tile tile = tiles[plane][sceneX][sceneY];
		if (tile == null)
		{
			return false;
		}
		for (GameObject current : tile.getGameObjects())
		{
			if (current == gameObject)
			{
				return true;
			}
		}
		return false;
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		cookingShrines.remove(gameObject);
		fireObjects.remove(gameObject);
		tetherPoints.remove(gameObject);
		ropeSupplies.remove(gameObject);
		hammerSupplies.remove(gameObject);
		bucketSupplies.remove(gameObject);
		reachedPathTargets.remove(gameObject);
		if (gameObject == guidancePathTarget)
		{
			clearGuidancePath();
		}
		if (gameObject == waveTetherTarget)
		{
			waveTetherTarget = null;
		}
	}

	private static boolean isFireObject(int objectId)
	{
		return objectId == ObjectID.TEMPOROSS_FIRE_VISUALS
			|| objectId == ObjectID.TEMPOROSS_LIGHTNING_SHADOW
			|| objectId == ObjectID.TEMPOROSS_LIGHTNING_SHADOW_SHORT;
	}

	private static int fireDuration(int objectId)
	{
		if (objectId == ObjectID.TEMPOROSS_LIGHTNING_SHADOW)
		{
			return FIRE_SPAWN_MILLIS;
		}
		if (objectId == ObjectID.TEMPOROSS_LIGHTNING_SHADOW_SHORT)
		{
			return FIRE_SPREADING_SPAWN_MILLIS;
		}
		return FIRE_SPREAD_MILLIS;
	}

	private void migrateTimerMode(String oldKey, String newKey)
	{
		String legacyValue = configManager.getConfiguration(TemporossCoachConfig.GROUP, oldKey);
		if (legacyValue == null)
		{
			return;
		}

		if (configManager.getConfiguration(TemporossCoachConfig.GROUP, newKey) == null)
		{
			TemporossTimerMode mode = Boolean.parseBoolean(legacyValue)
				? TemporossTimerMode.PIE
				: TemporossTimerMode.OFF;
			configManager.setConfiguration(TemporossCoachConfig.GROUP, newKey, mode);
		}
		configManager.unsetConfiguration(TemporossCoachConfig.GROUP, oldKey);
	}

	private static boolean isTetherPoint(int objectId)
	{
		switch (objectId)
		{
			case ObjectID.TEMPOROSS_MAST_BOTTOM_WEST:
			case ObjectID.TEMPOROSS_MAST_BOTTOM_EAST:
			case ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN:
			case ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN:
			case ObjectID.TEMPOROSS_MAST_BOTTOM_TETHERED:
			case ObjectID.TEMPOROSS_MAST_BOTTOM_UNTETHERED:
			case ObjectID.TEMPOROSS_TOTEM_NORTH:
			case ObjectID.TEMPOROSS_TOTEM_SOUTH:
			case ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN:
			case ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN:
			case ObjectID.TEMPOROSS_TOTEM_TETHERED:
			case ObjectID.TEMPOROSS_TOTEM_UNTETHERED:
				return true;
			default:
				return false;
		}
	}

	private void updateTrackedNpc(NPC npc)
	{
		boolean wasSelectedDoubleSpot = npc == guidancePathTarget;
		activeSpiritPools.remove(npc);
		fishingSpots.remove(npc);
		doubleFishingSpots.remove(npc);
		ammunitionCrates.remove(npc);

		if (npc.getId() == NpcID.TEMPOROSS_P2_FISHINGSPOT)
		{
			activeSpiritPools.add(npc);
		}
		else if (npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH
			|| npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SOUTH)
		{
			fishingSpots.add(npc);
		}
		else if (npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL)
		{
			doubleFishingSpots.add(npc);
		}
		else if (npc.getId() == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1
			|| npc.getId() == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_2
			|| npc.getId() == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_3
			|| npc.getId() == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_4)
		{
			ammunitionCrates.add(npc);
		}
		state.setSpiritPoolActive(!activeSpiritPools.isEmpty());
		state.setDoubleFishingSpotActive(!doubleFishingSpots.isEmpty());
		if (wasSelectedDoubleSpot && !doubleFishingSpots.contains(npc))
		{
			reachedPathTargets.remove(npc);
			clearGuidancePath();
		}
	}

	private void updateGuidancePath()
	{
		PathTargetKind pathKind = currentPathTargetKind();
		if (!state.isInEncounter() || pathKind == null || !isPathTargetInstruction())
		{
			clearGuidancePath();
			reachedPathTargets.clear();
			completedPathKind = null;
			activePathKind = null;
			return;
		}
		if (activePathKind != pathKind)
		{
			clearGuidancePath();
			reachedPathTargets.clear();
			completedPathKind = null;
			activePathKind = pathKind;
		}
		if (completedPathKind == pathKind)
		{
			clearGuidancePath();
			return;
		}
		if (client.getLocalPlayer() == null)
		{
			clearGuidancePath();
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		if (guidancePathTarget != null)
		{
			if (guidancePathDestination != null
				&& guidancePathDestination.equals(playerLocation))
			{
				reachedPathTargets.add(guidancePathTarget);
				completedPathKind = pathKind;
				clearGuidancePath();
				return;
			}
		}

		Object target = guidancePathTarget != null
			? guidancePathTarget
			: selectPathTarget();
		if (target == null)
		{
			clearGuidancePath();
			return;
		}
		WorldPoint destination = target == guidancePathTarget
			? guidancePathDestination
			: pathDestination(target, playerLocation);
		if (destination == null)
		{
			clearGuidancePath();
			return;
		}
		if (target != guidancePathTarget || !destination.equals(guidancePathDestination))
		{
			calculateGuidancePath(target, playerLocation, destination);
			return;
		}

		int playerIndex = guidancePath.indexOf(playerLocation);
		if (playerIndex > 0)
		{
			guidancePath = new ArrayList<>(guidancePath.subList(playerIndex, guidancePath.size()));
		}
		else if (playerIndex < 0 && !guidancePath.isEmpty())
		{
			calculateGuidancePath(target, playerLocation, destination);
		}
	}

	private boolean isPathTargetInstruction()
	{
		if (waveIncoming && !state.isTethered())
		{
			return config.pathToTetherPoint();
		}
		if (instruction.isDoubleFishingSpotPreferred())
		{
			return config.pathToDoubleFishingSpot();
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return config.pathToCookingShrine();
		}
		return instruction.getType() == TemporossInstruction.Type.LOAD
			&& config.pathToAmmunitionCrate();
	}

	private void updateWaveTetherTarget()
	{
		if (!waveIncoming)
		{
			waveTetherTarget = null;
			return;
		}
		if (waveTetherTarget != null && tetherPoints.contains(waveTetherTarget))
		{
			return;
		}
		Object nearest = nearestPathTarget(tetherPoints);
		waveTetherTarget = nearest instanceof GameObject ? (GameObject) nearest : null;
	}

	private PathTargetKind currentPathTargetKind()
	{
		if (waveIncoming && !state.isTethered())
		{
			return PathTargetKind.TETHER_POINT;
		}
		if (instruction.isDoubleFishingSpotPreferred())
		{
			return PathTargetKind.DOUBLE_SPOT;
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return PathTargetKind.COOKING_SHRINE;
		}
		if (instruction.getType() == TemporossInstruction.Type.LOAD)
		{
			return PathTargetKind.AMMUNITION_CRATE;
		}
		return null;
	}

	private Object selectPathTarget()
	{
		if (waveIncoming && !state.isTethered())
		{
			return waveTetherTarget;
		}
		if (instruction.isDoubleFishingSpotPreferred())
		{
			return nearestPathTarget(doubleFishingSpots);
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return nearestPathTarget(cookingShrines);
		}
		if (instruction.getType() == TemporossInstruction.Type.LOAD)
		{
			return NORTH_AMMUNITION_CRATE_INTERACTION_TILE;
		}
		return null;
	}

	private Object nearestPathTarget(Iterable<?> targets)
	{
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}
		Object nearest = null;
		int shortestPathLength = Integer.MAX_VALUE;
		for (Object target : targets)
		{
			if (reachedPathTargets.contains(target))
			{
				continue;
			}
			WorldArea area = pathTargetArea(target);
			if (area == null || area.getPlane() != playerLocation.getPlane())
			{
				continue;
			}
			WorldPoint destination = findWalkableAdjacentTile(area, playerLocation);
			if (destination == null)
			{
				continue;
			}
			List<WorldPoint> candidatePath = TemporossPathfinder.findPath(
				worldView, playerLocation, destination);
			if (!candidatePath.isEmpty() && candidatePath.size() < shortestPathLength)
			{
				nearest = target;
				shortestPathLength = candidatePath.size();
			}
		}
		return nearest;
	}

	private static WorldArea pathTargetArea(Object target)
	{
		if (target instanceof WorldPoint)
		{
			return new WorldArea((WorldPoint) target, 1, 1);
		}
		if (target instanceof NPC)
		{
			return ((NPC) target).getWorldArea();
		}
		if (target instanceof GameObject)
		{
			GameObject gameObject = (GameObject) target;
			return new WorldArea(gameObject.getWorldLocation(), gameObject.sizeX(), gameObject.sizeY());
		}
		return null;
	}

	private static WorldPoint pathTargetLocation(Object target)
	{
		if (target instanceof WorldPoint)
		{
			return (WorldPoint) target;
		}
		return target instanceof NPC
			? ((NPC) target).getWorldLocation()
			: ((GameObject) target).getWorldLocation();
	}

	private WorldPoint pathDestination(Object target, WorldPoint playerLocation)
	{
		if (!(target instanceof WorldPoint))
		{
			return findWalkableAdjacentTile(pathTargetArea(target), playerLocation);
		}

		WorldPoint templatePoint = (WorldPoint) target;
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null || !worldView.isInstance())
		{
			return templatePoint;
		}

		WorldPoint nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (WorldPoint instancePoint : WorldPoint.toLocalInstance(worldView, templatePoint))
		{
			if (instancePoint.getPlane() != playerLocation.getPlane()
				|| LocalPoint.fromWorld(worldView, instancePoint) == null)
			{
				continue;
			}
			int distance = instancePoint.distanceTo(playerLocation);
			if (distance < nearestDistance)
			{
				nearest = instancePoint;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private void calculateGuidancePath(Object target, WorldPoint start, WorldPoint destination)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return;
		}
		guidancePathTarget = target;
		guidancePathDestination = destination;
		guidancePath = TemporossPathfinder.findPath(worldView, start, destination);
		log.debug("Calculated guidance path start={} target={} destination={} tiles={}",
			start, pathTargetLocation(target), destination, guidancePath.size());
	}

	private WorldPoint findWalkableAdjacentTile(WorldArea area, WorldPoint playerLocation)
	{
		if (area == null)
		{
			return null;
		}
		WorldPoint nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (int x = area.getX() - 1; x <= area.getX() + area.getWidth(); x++)
		{
			for (int y = area.getY() - 1; y <= area.getY() + area.getHeight(); y++)
			{
				WorldPoint candidate = new WorldPoint(x, y, area.getPlane());
				if (area.contains(candidate) || !isWalkable(candidate))
				{
					continue;
				}
				int distance = candidate.distanceTo(playerLocation);
				if (distance < nearestDistance)
				{
					nearest = candidate;
					nearestDistance = distance;
				}
			}
		}
		return nearest;
	}

	private boolean isWalkable(WorldPoint worldPoint)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return false;
		}
		LocalPoint localPoint = LocalPoint.fromWorld(worldView, worldPoint);
		CollisionData[] collisionMaps = worldView.getCollisionMaps();
		if (localPoint == null || collisionMaps == null
			|| worldPoint.getPlane() < 0 || worldPoint.getPlane() >= collisionMaps.length
			|| collisionMaps[worldPoint.getPlane()] == null)
		{
			return false;
		}

		int[][] flags = collisionMaps[worldPoint.getPlane()].getFlags();
		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		return sceneX >= 0 && sceneX < flags.length
			&& sceneY >= 0 && sceneY < flags[sceneX].length
			&& (flags[sceneX][sceneY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
	}

	private void clearGuidancePath()
	{
		guidancePathTarget = null;
		guidancePathDestination = null;
		guidancePath = Collections.emptyList();
	}

	TemporossState getState()
	{
		return state;
	}

	TemporossInstruction getInstruction()
	{
		return instruction;
	}

	List<WorldPoint> getGuidancePath()
	{
		return guidancePath;
	}

	Object getGuidancePathTarget()
	{
		return guidancePathTarget;
	}

	GameObject getWaveTetherTarget()
	{
		return waveTetherTarget;
	}

	private enum PathTargetKind
	{
		TETHER_POINT,
		DOUBLE_SPOT,
		COOKING_SHRINE,
		AMMUNITION_CRATE
	}

	Set<GameObject> getCookingShrines()
	{
		return cookingShrines;
	}

	Set<NPC> getFishingSpots()
	{
		return fishingSpots;
	}

	Set<NPC> getDoubleFishingSpots()
	{
		return doubleFishingSpots;
	}

	Set<NPC> getAmmunitionCrates()
	{
		return ammunitionCrates;
	}

	Set<NPC> getActiveSpiritPools()
	{
		return activeSpiritPools;
	}

	Map<GameObject, TemporossTimedObject> getFireObjects()
	{
		return fireObjects;
	}

	Set<GameObject> getTetherPoints()
	{
		return tetherPoints;
	}

	boolean isWaveIncoming()
	{
		return waveIncoming;
	}

	Instant getWaveImpactTime()
	{
		return waveImpactTime;
	}

	Set<GameObject> getRopeSupplies()
	{
		return ropeSupplies;
	}

	Set<GameObject> getHammerSupplies()
	{
		return hammerSupplies;
	}

	Set<GameObject> getBucketSupplies()
	{
		return bucketSupplies;
	}
}
