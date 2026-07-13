package com.temporossfirefighter;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameObject;
import net.runelite.api.ItemContainer;
import net.runelite.api.Item;
import net.runelite.api.EquipmentInventorySlot;
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
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
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
	name = "Tempoross Firefighter",
	description = "Guidance and inventory tracking for Tempoross",
	tags = {"tempoross", "fishing", "firefighter", "helper"}
)
public class TemporossFirefighterPlugin extends Plugin
{
	private static final String LEGACY_CONFIG_GROUP = "tempoross-coach";
	private static final String WAVE_INCOMING_MESSAGE = "a colossal wave closes in...";
	private static final String WAVE_END_SAFE_MESSAGE = "as the wave washes over you";
	private static final String WAVE_END_DANGEROUS_MESSAGE = "the wave slams into you";
	private static final String LOAD_STARTED_MESSAGE = "you start loading harpoonfish into the cannon";
	static final int WAVE_IMPACT_MILLIS = 7_800;
	private static final int FIRE_SPREAD_MILLIS = 24_000;
	private static final int FIRE_SPAWN_MILLIS = 9_600;
	private static final int FIRE_SPREADING_SPAWN_MILLIS = 1_200;
	private static final int SHIP_FIRE_MAX_DISTANCE = 8;
	private static final int MAX_FIREFIGHTING_TARGETS = 8;
	private static final WorldPoint NORTH_AMMUNITION_CRATE_INTERACTION_TILE =
		new WorldPoint(3037, 2850, 0);

	@Inject
	private Client client;

	@Inject
	private Notifier notifier;

	@Inject
	private TemporossFirefighterConfig config;

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
	private final TemporossLoadTracker loadTracker = new TemporossLoadTracker();
	private final Set<NPC> activeSpiritPools = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> inactiveSpiritPools = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> fishingSpots = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> doubleFishingSpots = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<NPC> ammunitionCrates = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<GameObject> cookingShrines = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Map<GameObject, TemporossTimedObject> fireObjects = new IdentityHashMap<>();
	private final Set<GameObject> shipFires = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<GameObject> pathFires = Collections.newSetFromMap(new IdentityHashMap<>());
	private List<GameObject> firefightingTargets = Collections.emptyList();
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
	private Object completedPathTarget;
	private PathTargetKind activePathKind;
	private GameObject waveTetherTarget;
	private NPC normalFishingSpotTarget;
	private TemporossInstruction instruction = TemporossStrategy.placeholderInstruction();
	private String targetNotificationKey;

	@Provides
	TemporossFirefighterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TemporossFirefighterConfig.class);
	}

	@Override
	protected void startUp()
	{
		migrateConfigGroup();
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
		log.debug("Tempoross Firefighter started");
	}

	@Override
	protected void shutDown()
	{
		clearGuidancePath();
		completedPathTarget = null;
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
		inactiveSpiritPools.clear();
		fishingSpots.clear();
		doubleFishingSpots.clear();
		normalFishingSpotTarget = null;
		ammunitionCrates.clear();
		cookingShrines.clear();
		fireObjects.clear();
		shipFires.clear();
		pathFires.clear();
		firefightingTargets = Collections.emptyList();
		tetherPoints.clear();
		ropeSupplies.clear();
		hammerSupplies.clear();
		bucketSupplies.clear();
		waveIncoming = false;
		loadTracker.reset();
		wasInEncounter = false;
		waveImpactTime = null;
		state.clear();
		strategy.reset();
		instruction = TemporossStrategy.placeholderInstruction();
		targetNotificationKey = null;
		log.debug("Tempoross Firefighter stopped");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		state.updateInventory(inventory);
		state.setRopePresent(
			state.getRope() >= 1
				|| hasFullSpiritAngler(equipment));
		state.setHammerPresent(
			state.getHammer() >= 1
				|| hasEquippedImcandoHammer(equipment));
		state.setHarpoonPresent(
			containsHarpoon(inventory)
				|| weaponIsHarpoon(equipment));
		updateHumidifyCastable();
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
			inactiveSpiritPools.clear();
			waveIncoming = false;
			waveImpactTime = null;
		}
		state.setSpiritPoolActive(!activeSpiritPools.isEmpty());
		state.setDoubleFishingSpotActive(!doubleFishingSpots.isEmpty());
		updateShipFires();
		state.setTethered(client.getVarbitValue(VarbitID.TEMPOROSS_TETHERED) > 0);
		updateLoadingState();
		pruneStaleTetherPoints();
		TemporossInstruction nextInstruction = strategy.getInstruction(state);
		updateTargetNotification(nextInstruction);
		instruction = nextInstruction;
		updateWaveTetherTarget();
		updateGuidancePath();
		updatePathFires();
		updateFirefightingTargets();
		wasInEncounter = state.isInEncounter();
	}

	private void updateTargetNotification(TemporossInstruction nextInstruction)
	{
		String nextKey = nextInstruction.getType() + "|" + nextInstruction.getTitle()
			+ "|" + nextInstruction.isDoubleFishingSpotPreferred();
		if (targetNotificationKey != null
			&& !targetNotificationKey.equals(nextKey)
			&& config.targetNotification().isEnabled())
		{
			notifier.notify(config.targetNotification(), "New target: " + nextInstruction.getTitle());
		}
		targetNotificationKey = nextKey;
	}

	private void updateLoadingState()
	{
		if (!state.isInEncounter() || client.getLocalPlayer() == null)
		{
			loadTracker.reset();
			state.setLoadingFish(false);
			return;
		}

		Actor interacting = client.getLocalPlayer().getInteracting();
		boolean interactingWithCrate = interacting instanceof NPC
			&& isAmmunitionCrate(((NPC) interacting).getId());
		state.setLoadingFish(loadTracker.update(client.getTickCount(), interactingWithCrate));
	}

	private void updateHumidifyCastable()
	{
		refreshSpellbookState();
		Widget humidify = client.getWidget(InterfaceID.MagicSpellbook.HUMIDIFY);
		state.setHumidifyCastable(humidify != null
			&& isHumidifyCastableState(humidify.getSpriteId(), humidify.isSelfHidden()));
	}

	private void refreshSpellbookState()
	{
		Widget spellbook = client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
		if (spellbook != null && spellbook.getOnInvTransmitListener() != null)
		{
			client.createScriptEventBuilder(spellbook.getOnInvTransmitListener())
				.setSource(spellbook)
				.build()
				.run();
		}
	}

	static boolean isHumidifyCastableState(Integer spriteId, boolean selfHidden)
	{
		return !selfHidden
			&& spriteId != null
			&& spriteId != SpriteID.LunarMagicOff.HUMIDIFY;
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

	static boolean hasFullSpiritAngler(ItemContainer equipment)
	{
		if (equipment == null)
		{
			return false;
		}

		boolean hat = false;
		boolean top = false;
		boolean legs = false;
		boolean boots = false;
		for (Item item : equipment.getItems())
		{
			if (item == null || item.getQuantity() <= 0)
			{
				continue;
			}

			switch (item.getId())
			{
				case ItemID.SPIRIT_ANGLER_HAT:
					hat = true;
					break;
				case ItemID.SPIRIT_ANGLER_TOP:
					top = true;
					break;
				case ItemID.SPIRIT_ANGLER_LEGS:
					legs = true;
					break;
				case ItemID.SPIRIT_ANGLER_BOOTS:
					boots = true;
					break;
				default:
					break;
			}
		}
		return hat && top && legs && boots;
	}

	static boolean hasEquippedImcandoHammer(ItemContainer equipment)
	{
		if (equipment == null)
		{
			return false;
		}

		for (Item item : equipment.getItems())
		{
			if (item != null && item.getQuantity() > 0
				&& (item.getId() == ItemID.IMCANDO_HAMMER
					|| item.getId() == ItemID.IMCANDO_HAMMER_OFFHAND))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isHarpoon(Item item)
	{
		if (isKnownHarpoonId(item.getId()))
		{
			return true;
		}

		String name = itemManager.getItemComposition(item.getId()).getName();
		return name != null && name.toLowerCase(java.util.Locale.ROOT).contains("harpoon");
	}

	static boolean isKnownHarpoonId(int itemId)
	{
		switch (itemId)
		{
			case ItemID.HARPOON:
			case ItemID.HUNTING_BARBED_HARPOON:
			case ItemID.DRAGON_HARPOON:
			case ItemID.INFERNAL_HARPOON:
			case ItemID.INFERNAL_HARPOON_EMPTY:
			case ItemID.CRYSTAL_HARPOON:
			case ItemID.CRYSTAL_HARPOON_INACTIVE:
			case ItemID.CRYSTAL_HARPOON_FISHING:
				return true;
			default:
				return false;
		}
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
			&& state.isWaterBucketRatioLow();
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
		completedPathTarget = null;
		activeSpiritPools.clear();
		inactiveSpiritPools.clear();
		fishingSpots.clear();
		doubleFishingSpots.clear();
		normalFishingSpotTarget = null;
		ammunitionCrates.clear();
		cookingShrines.clear();
		fireObjects.clear();
		shipFires.clear();
		pathFires.clear();
		firefightingTargets = Collections.emptyList();
		tetherPoints.clear();
		ropeSupplies.clear();
		hammerSupplies.clear();
		bucketSupplies.clear();
		loadTracker.reset();
		state.setLoadingFish(false);
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!state.isInEncounter() || event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		String message = event.getMessage().toLowerCase();
		if (message.contains(LOAD_STARTED_MESSAGE))
		{
			loadTracker.observeActivity(client.getTickCount());
		}
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
		NPC npc = event.getNpc();
		if (npc == guidancePathTarget)
		{
			clearGuidancePath();
		}
		if (npc == completedPathTarget)
		{
			completedPathTarget = null;
		}
		if (npc == normalFishingSpotTarget)
		{
			normalFishingSpotTarget = null;
		}
		updateTrackedNpc(npc);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		activeSpiritPools.remove(npc);
		inactiveSpiritPools.remove(npc);
		fishingSpots.remove(npc);
		doubleFishingSpots.remove(npc);
		if (npc == normalFishingSpotTarget)
		{
			normalFishingSpotTarget = null;
		}
		if (npc == completedPathTarget)
		{
			completedPathTarget = null;
		}
		if (npc == guidancePathTarget)
		{
			clearGuidancePath();
		}
		ammunitionCrates.remove(npc);
		state.setSpiritPoolActive(!activeSpiritPools.isEmpty());
		state.setDoubleFishingSpotActive(!doubleFishingSpots.isEmpty());
		if (normalFishingSpotTarget != null && !fishingSpots.contains(normalFishingSpotTarget))
		{
			normalFishingSpotTarget = null;
		}
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
		shipFires.remove(gameObject);
		pathFires.remove(gameObject);
		firefightingTargets.remove(gameObject);
		tetherPoints.remove(gameObject);
		ropeSupplies.remove(gameObject);
		hammerSupplies.remove(gameObject);
		bucketSupplies.remove(gameObject);
		if (gameObject == completedPathTarget)
		{
			completedPathTarget = null;
		}
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
		shipFires.remove(gameObject);
		pathFires.remove(gameObject);
		firefightingTargets.remove(gameObject);
		tetherPoints.remove(gameObject);
		ropeSupplies.remove(gameObject);
		hammerSupplies.remove(gameObject);
		bucketSupplies.remove(gameObject);
		if (gameObject == completedPathTarget)
		{
			completedPathTarget = null;
		}
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

	private void updateShipFires()
	{
		shipFires.clear();
		if (client.getLocalPlayer() == null)
		{
			state.setShipFireActive(false);
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		WorldPoint shipAnchor = pathDestination(
			NORTH_AMMUNITION_CRATE_INTERACTION_TILE, playerLocation);
		if (shipAnchor != null)
		{
			for (GameObject fire : fireObjects.keySet())
			{
				if (fire.getId() == ObjectID.TEMPOROSS_FIRE_VISUALS
					&& fire.getWorldLocation().distanceTo(shipAnchor) <= SHIP_FIRE_MAX_DISTANCE)
				{
					shipFires.add(fire);
				}
			}
		}
		state.setShipFireActive(!shipFires.isEmpty());
	}

	private void updatePathFires()
	{
		pathFires.clear();
		if (guidancePath.isEmpty())
		{
			return;
		}

		Set<WorldPoint> pathTiles = new HashSet<>(guidancePath);
		for (GameObject fire : fireObjects.keySet())
		{
			if (fire.getId() != ObjectID.TEMPOROSS_FIRE_VISUALS)
			{
				continue;
			}
			WorldArea fireArea = pathTargetArea(fire);
			if (areaIntersectsPath(fireArea, pathTiles))
			{
				pathFires.add(fire);
			}
		}
	}

	private void updateFirefightingTargets()
	{
		if (instruction.getType() != TemporossInstruction.Type.DOUSE_ALL_FIRES
			|| client.getLocalPlayer() == null)
		{
			firefightingTargets = Collections.emptyList();
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		List<GameObject> targets = new ArrayList<>();
		for (GameObject fire : fireObjects.keySet())
		{
			if (fire.getId() == ObjectID.TEMPOROSS_FIRE_VISUALS)
			{
				targets.add(fire);
			}
		}
		targets.sort((first, second) -> Integer.compare(
			first.getWorldLocation().distanceTo(playerLocation),
			second.getWorldLocation().distanceTo(playerLocation)));
		if (targets.size() > MAX_FIREFIGHTING_TARGETS)
		{
			targets = new ArrayList<>(targets.subList(0, MAX_FIREFIGHTING_TARGETS));
		}
		firefightingTargets = targets;
	}

	static boolean areaIntersectsPath(WorldArea area, Iterable<WorldPoint> pathTiles)
	{
		if (area == null)
		{
			return false;
		}
		for (WorldPoint pathTile : pathTiles)
		{
			if (area.contains(pathTile))
			{
				return true;
			}
		}
		return false;
	}

	private void migrateTimerMode(String oldKey, String newKey)
	{
		String legacyValue = configManager.getConfiguration(TemporossFirefighterConfig.GROUP, oldKey);
		if (legacyValue == null)
		{
			return;
		}

		if (configManager.getConfiguration(TemporossFirefighterConfig.GROUP, newKey) == null)
		{
			TemporossTimerMode mode = Boolean.parseBoolean(legacyValue)
				? TemporossTimerMode.PIE
				: TemporossTimerMode.OFF;
			configManager.setConfiguration(TemporossFirefighterConfig.GROUP, newKey, mode);
		}
		configManager.unsetConfiguration(TemporossFirefighterConfig.GROUP, oldKey);
	}

	private void migrateConfigGroup()
	{
		String legacyPrefix = LEGACY_CONFIG_GROUP + ".";
		for (String fullKey : configManager.getConfigurationKeys(legacyPrefix))
		{
			String key = fullKey.substring(legacyPrefix.length());
			String legacyValue = configManager.getConfiguration(LEGACY_CONFIG_GROUP, key);
			if (legacyValue == null)
			{
				continue;
			}

			if (configManager.getConfiguration(TemporossFirefighterConfig.GROUP, key) == null)
			{
				configManager.setConfiguration(TemporossFirefighterConfig.GROUP, key, legacyValue);
			}
			configManager.unsetConfiguration(LEGACY_CONFIG_GROUP, key);
		}
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

	private static boolean isAmmunitionCrate(int npcId)
	{
		return npcId == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_1
			|| npcId == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_2
			|| npcId == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_3
			|| npcId == NpcID.TEMPOROSS_NPC_CRATE_AMMUNITION_4;
	}

	private void updateTrackedNpc(NPC npc)
	{
		activeSpiritPools.remove(npc);
		inactiveSpiritPools.remove(npc);
		fishingSpots.remove(npc);
		doubleFishingSpots.remove(npc);
		ammunitionCrates.remove(npc);

		if (npc.getId() == NpcID.TEMPOROSS_P2_FISHINGSPOT)
		{
			activeSpiritPools.add(npc);
		}
		else if (npc.getId() == NpcID.TEMPOROSS_P2_FISHINGSPOT_INACTIVE)
		{
			inactiveSpiritPools.add(npc);
		}
		else if (npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_NORTH
			|| npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SOUTH)
		{
			fishingSpots.add(npc);
		}
		else if (npc.getId() == NpcID.TEMPOROSS_HARPOONFISH_FISHINGSPOT_SPECIAL)
		{
			doubleFishingSpots.add(npc);
			completedPathTarget = null;
			normalFishingSpotTarget = null;
			clearGuidancePath();
		}
		else if (isAmmunitionCrate(npc.getId()))
		{
			ammunitionCrates.add(npc);
		}
		state.setSpiritPoolActive(!activeSpiritPools.isEmpty());
		state.setDoubleFishingSpotActive(!doubleFishingSpots.isEmpty());
		if (normalFishingSpotTarget != null && !fishingSpots.contains(normalFishingSpotTarget))
		{
			normalFishingSpotTarget = null;
		}
	}

	private void updateGuidancePath()
	{
		PathTargetKind pathKind = currentPathTargetKind();
		if (!state.isInEncounter() || pathKind == null || !isPathTargetInstruction())
		{
			clearGuidancePath();
			completedPathTarget = null;
			activePathKind = null;
			normalFishingSpotTarget = null;
			return;
		}
		if (activePathKind != pathKind)
		{
			clearGuidancePath();
			completedPathTarget = null;
			activePathKind = pathKind;
			if (pathKind != PathTargetKind.FISHING_SPOT)
			{
				normalFishingSpotTarget = null;
			}
		}
		if (client.getLocalPlayer() == null)
		{
			clearGuidancePath();
			return;
		}

		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
		if (guidancePathTarget != null)
		{
			if (isAdjacentToPathTarget(guidancePathTarget, playerLocation))
			{
				completedPathTarget = guidancePathTarget;
				clearGuidancePath();
				return;
			}
		}

		Object target = guidancePathTarget != null
			? guidancePathTarget
			: selectPathTarget();
		if (target == null)
		{
			log.debug("No guidance path target found for kind={} instruction={} doubles={} normals={}",
				pathKind, instruction.getTitle(), doubleFishingSpots.size(), fishingSpots.size());
			clearGuidancePath();
			return;
		}
		if (target == completedPathTarget)
		{
			clearGuidancePath();
			return;
		}
		WorldPoint destination = target == guidancePathTarget
			? guidancePathDestination
			: pathDestination(target, playerLocation);
		if (destination == null)
		{
			log.debug("No walkable guidance destination for kind={} target={}",
				pathKind, pathTargetLocation(target));
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
		if (waveIncoming)
		{
			return !state.isTethered() && config.pathToTetherPoint();
		}
		if (shouldPreferDoubleFishingSpot())
		{
			return config.pathToDoubleFishingSpot();
		}
		if (isMissingSupplyInstruction())
		{
			return config.pathToMissingSupplies();
		}
		if (instruction.getType() == TemporossInstruction.Type.FISH)
		{
			return config.pathToFishingSpot();
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return config.pathToCookingShrine();
		}
		if (instruction.getType() == TemporossInstruction.Type.ATTACK)
		{
			return hasSpiritPoolPathTarget() && config.pathToSpiritPool();
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
		if (waveIncoming)
		{
			return state.isTethered() ? null : PathTargetKind.TETHER_POINT;
		}
		if (shouldPreferDoubleFishingSpot())
		{
			return PathTargetKind.DOUBLE_SPOT;
		}
		if (isMissingSupplyInstruction())
		{
			return PathTargetKind.MISSING_SUPPLY;
		}
		if (instruction.getType() == TemporossInstruction.Type.FISH)
		{
			return PathTargetKind.FISHING_SPOT;
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return PathTargetKind.COOKING_SHRINE;
		}
		if (instruction.getType() == TemporossInstruction.Type.LOAD)
		{
			return PathTargetKind.AMMUNITION_CRATE;
		}
		if (instruction.getType() == TemporossInstruction.Type.ATTACK
			&& hasSpiritPoolPathTarget())
		{
			return PathTargetKind.SPIRIT_POOL;
		}
		return null;
	}

	private Object selectPathTarget()
	{
		if (waveIncoming)
		{
			return state.isTethered() ? null : waveTetherTarget;
		}
		if (shouldPreferDoubleFishingSpot())
		{
			return nearestPathTarget(doubleFishingSpots);
		}
		if (instruction.getType() == TemporossInstruction.Type.GET_ROPE)
		{
			return nearestPathTarget(ropeSupplies);
		}
		if (instruction.getType() == TemporossInstruction.Type.GET_HAMMER)
		{
			return nearestPathTarget(hammerSupplies);
		}
		if (instruction.getType() == TemporossInstruction.Type.GET_BUCKETS
			|| instruction.getType() == TemporossInstruction.Type.FILL_BUCKETS)
		{
			return nearestPathTarget(bucketSupplies);
		}
		if (instruction.getType() == TemporossInstruction.Type.FISH)
		{
			Object target = nearestPathTarget(fishingSpots);
			normalFishingSpotTarget = target instanceof NPC ? (NPC) target : null;
			return target;
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return nearestPathTarget(cookingShrines);
		}
		if (instruction.getType() == TemporossInstruction.Type.LOAD)
		{
			if (strategy.getStage() == TemporossStage.LOAD_FINAL_FISH)
			{
				return nearestPathTarget(ammunitionCrates);
			}
			return NORTH_AMMUNITION_CRATE_INTERACTION_TILE;
		}
		if (instruction.getType() == TemporossInstruction.Type.ATTACK)
		{
			return getPathSpiritPool();
		}
		return null;
	}

	private boolean hasSpiritPoolPathTarget()
	{
		return !activeSpiritPools.isEmpty()
			|| (state.getEnergyPercent() != null
				&& state.getEnergyPercent() <= 0
				&& !inactiveSpiritPools.isEmpty());
	}

	private NPC getPathSpiritPool()
	{
		if (!activeSpiritPools.isEmpty())
		{
			return getTargetSpiritPool();
		}
		if (state.getEnergyPercent() == null || state.getEnergyPercent() > 0)
		{
			return null;
		}
		if (strategy.getStage() == TemporossStage.FINISH_TEMPOROSS)
		{
			Object nearest = nearestPathTarget(inactiveSpiritPools);
			return nearest instanceof NPC ? (NPC) nearest : null;
		}
		return getNorthernSpiritPool(inactiveSpiritPools);
	}

	private boolean shouldPreferDoubleFishingSpot()
	{
		return instruction.getType() == TemporossInstruction.Type.FISH
			&& !doubleFishingSpots.isEmpty();
	}

	private boolean isMissingSupplyInstruction()
	{
		return instruction.getType() == TemporossInstruction.Type.GET_ROPE
			|| instruction.getType() == TemporossInstruction.Type.GET_HAMMER
			|| instruction.getType() == TemporossInstruction.Type.GET_BUCKETS
			|| instruction.getType() == TemporossInstruction.Type.FILL_BUCKETS;
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
			WorldArea area = pathTargetArea(target);
			if (area == null || area.getPlane() != playerLocation.getPlane())
			{
				continue;
			}
			WorldPoint destination = findReachableAdjacentTile(area, playerLocation);
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

	private static boolean isAdjacentToPathTarget(Object target, WorldPoint playerLocation)
	{
		WorldArea area = pathTargetArea(target);
		if (area == null || area.getPlane() != playerLocation.getPlane())
		{
			return false;
		}

		int maximumX = area.getX() + area.getWidth() - 1;
		int maximumY = area.getY() + area.getHeight() - 1;
		int xDistance = Math.max(0,
			Math.max(area.getX() - playerLocation.getX(), playerLocation.getX() - maximumX));
		int yDistance = Math.max(0,
			Math.max(area.getY() - playerLocation.getY(), playerLocation.getY() - maximumY));
		return Math.max(xDistance, yDistance) <= 1;
	}

	private WorldPoint pathDestination(Object target, WorldPoint playerLocation)
	{
		if (!(target instanceof WorldPoint))
		{
			return findReachableAdjacentTile(pathTargetArea(target), playerLocation);
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

	private WorldPoint findReachableAdjacentTile(WorldArea area, WorldPoint playerLocation)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (area == null || worldView == null)
		{
			return null;
		}
		WorldPoint nearest = null;
		int shortestPathLength = Integer.MAX_VALUE;
		for (int x = area.getX() - 1; x <= area.getX() + area.getWidth(); x++)
		{
			for (int y = area.getY() - 1; y <= area.getY() + area.getHeight(); y++)
			{
				WorldPoint candidate = new WorldPoint(x, y, area.getPlane());
				if (area.contains(candidate) || !isWalkable(candidate))
				{
					continue;
				}
				List<WorldPoint> path = TemporossPathfinder.findPath(
					worldView, playerLocation, candidate);
				if (!path.isEmpty() && path.size() < shortestPathLength)
				{
					nearest = candidate;
					shortestPathLength = path.size();
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
		pathFires.clear();
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

	NPC getNormalFishingSpotTarget()
	{
		return normalFishingSpotTarget;
	}

	GameObject getWaveTetherTarget()
	{
		return waveTetherTarget;
	}

	private enum PathTargetKind
	{
		TETHER_POINT,
		MISSING_SUPPLY,
		FISHING_SPOT,
		DOUBLE_SPOT,
		COOKING_SHRINE,
		AMMUNITION_CRATE,
		SPIRIT_POOL
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

	NPC getNorthernSpiritPool()
	{
		return getNorthernSpiritPool(activeSpiritPools);
	}

	private static NPC getNorthernSpiritPool(Iterable<NPC> spiritPools)
	{
		NPC northern = null;
		int northernEdge = Integer.MIN_VALUE;
		for (NPC npc : spiritPools)
		{
			int edge = npc.getWorldArea().getY() + npc.getWorldArea().getHeight() - 1;
			if (edge > northernEdge)
			{
				northern = npc;
				northernEdge = edge;
			}
		}
		return northern;
	}

	NPC getTargetSpiritPool()
	{
		if (strategy.getStage() == TemporossStage.FINISH_TEMPOROSS)
		{
			Object nearest = nearestPathTarget(activeSpiritPools);
			return nearest instanceof NPC ? (NPC) nearest : null;
		}
		return getNorthernSpiritPool();
	}

	Map<GameObject, TemporossTimedObject> getFireObjects()
	{
		return fireObjects;
	}

	Set<GameObject> getShipFires()
	{
		return shipFires;
	}

	Set<GameObject> getPathFires()
	{
		return pathFires;
	}

	List<GameObject> getFirefightingTargets()
	{
		return firefightingTargets;
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
