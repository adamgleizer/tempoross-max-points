package com.temporosscoach;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;

@ConfigGroup(TemporossCoachConfig.GROUP)
public interface TemporossCoachConfig extends Config
{
	String GROUP = "tempoross-coach";

	@ConfigSection(
		name = "Scene highlights",
		description = "Configure guidance highlights in the game scene",
		position = 1
	)
	String sceneHighlights = "sceneHighlights";

	@ConfigItem(
		keyName = "fireTimerMode",
		name = "Highlight fires",
		description = "Highlights fire tiles and optionally shows pie, tick, or second timers",
		section = sceneHighlights,
		position = 2
	)
	default TemporossTimerMode fireTimerMode()
	{
		return TemporossTimerMode.PIE;
	}

	@Alpha
	@ConfigItem(
		keyName = "fireColor",
		name = "Fire color",
		description = "Color of active fire highlights",
		section = sceneHighlights,
		position = 3
	)
	default Color fireColor()
	{
		return new Color(255, 165, 0, 80);
	}

	@ConfigItem(
		keyName = "highlightTargetShipFire",
		name = "Highlight target ship fire",
		description = "Adds a clickbox highlight to ship fires when dousing one is the current target",
		section = sceneHighlights,
		position = 4
	)
	default boolean highlightTargetShipFire()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "targetShipFireColor",
		name = "Target ship fire color",
		description = "Color of the target ship fire clickbox highlight",
		section = sceneHighlights,
		position = 5
	)
	default Color targetShipFireColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightPathFires",
		name = "Highlight fires in path",
		description = "Adds a clickbox highlight to active fires intersecting the current target path",
		section = sceneHighlights,
		position = 6
	)
	default boolean highlightPathFires()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "pathFireColor",
		name = "Path fire color",
		description = "Color of fire clickbox highlights along the current target path",
		section = sceneHighlights,
		position = 7
	)
	default Color pathFireColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightFishingSpots",
		name = "Highlight fishing spot",
		description = "Highlights the nearest normal harpoonfish spot while fishing",
		section = sceneHighlights,
		position = 4
	)
	default boolean highlightFishingSpots()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "fishingSpotColor",
		name = "Fishing spot color",
		description = "Color of the nearest normal harpoonfish spot highlight",
		section = sceneHighlights,
		position = 5
	)
	default Color fishingSpotColor()
	{
		return new Color(0, 0, 139);
	}

	@ConfigItem(
		keyName = "highlightDoubleFishingSpots",
		name = "Highlight double spot",
		description = "Highlights double harpoonfish spots while fishing",
		section = sceneHighlights,
		position = 6
	)
	default boolean highlightDoubleFishingSpots()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "doubleFishingSpotColor",
		name = "Double spot color",
		description = "Color of double harpoonfish spot highlights",
		section = sceneHighlights,
		position = 7
	)
	default Color doubleFishingSpotColor()
	{
		return new Color(0, 100, 0);
	}

	@ConfigItem(
		keyName = "highlightCookingShrines",
		name = "Highlight cooking shrine",
		description = "Highlights cooking shrines while cooking fish",
		section = sceneHighlights,
		position = 9
	)
	default boolean highlightCookingShrines()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "cookingShrineColor",
		name = "Cooking shrine color",
		description = "Color of cooking shrine highlights",
		section = sceneHighlights,
		position = 10
	)
	default Color cookingShrineColor()
	{
		return new Color(0, 200, 220);
	}

	@ConfigItem(
		keyName = "highlightAmmunitionCrates",
		name = "Highlight ammunition crate",
		description = "Highlights the nearest ammunition crate while loading fish",
		section = sceneHighlights,
		position = 11
	)
	default boolean highlightAmmunitionCrates()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "ammunitionCrateColor",
		name = "Ammunition crate color",
		description = "Color of the ammunition crate highlight",
		section = sceneHighlights,
		position = 12
	)
	default Color ammunitionCrateColor()
	{
		return new Color(0, 200, 220);
	}

	@ConfigItem(
		keyName = "highlightDamagedTetherPoints",
		name = "Highlight damaged poles",
		description = "Highlights damaged masts and totem poles",
		section = sceneHighlights,
		position = 13
	)
	default boolean highlightDamagedTetherPoints()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "damagedTetherPointColor",
		name = "Damaged pole color",
		description = "Color of damaged mast and totem pole highlights",
		section = sceneHighlights,
		position = 14
	)
	default Color damagedTetherPointColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "highlightMissingSupplies",
		name = "Highlight missing supplies",
		description = "Highlights rope, hammer, or bucket supplies when they are the current target",
		section = sceneHighlights,
		position = 15
	)
	default boolean highlightMissingSupplies()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "missingSupplyColor",
		name = "Missing supply color",
		description = "Color of rope, hammer, and bucket supply highlights",
		section = sceneHighlights,
		position = 16
	)
	default Color missingSupplyColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "highlightSpiritPool",
		name = "Highlight spirit pool",
		description = "Highlights the spirit pool when attacking Tempoross is the current target",
		section = sceneHighlights,
		position = 17
	)
	default boolean highlightSpiritPool()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "spiritPoolColor",
		name = "Spirit pool color",
		description = "Color of the spirit pool highlight",
		section = sceneHighlights,
		position = 18
	)
	default Color spiritPoolColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "highlightHumidifySpell",
		name = "Highlight Humidify",
		description = "Highlights Humidify in the spellbook when two or fewer water buckets remain",
		section = sceneHighlights,
		position = 19
	)
	default boolean highlightHumidifySpell()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "humidifySpellColor",
		name = "Humidify color",
		description = "Color of the Humidify spellbook highlight",
		section = sceneHighlights,
		position = 20
	)
	default Color humidifySpellColor()
	{
		return Color.CYAN;
	}

	@ConfigSection(
		name = "Target paths",
		description = "Configure routes to the current strategy target",
		position = 21
	)
	String targetPaths = "targetPaths";

	@ConfigItem(
		keyName = "pathToFishingSpot",
		name = "Path to fishing spot",
		description = "Draws a route when a normal fishing spot is the current target",
		section = targetPaths,
		position = 22
	)
	default boolean pathToFishingSpot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathToDoubleFishingSpot",
		name = "Path to double spot",
		description = "Draws a route when a double fishing spot is the current target",
		section = targetPaths,
		position = 23
	)
	default boolean pathToDoubleFishingSpot()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathToCookingShrine",
		name = "Path to cooking shrine",
		description = "Draws a route when a cooking shrine is the current target",
		section = targetPaths,
		position = 24
	)
	default boolean pathToCookingShrine()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathToAmmunitionCrate",
		name = "Path to ammunition crate",
		description = "Draws a route when an ammunition crate is the current target",
		section = targetPaths,
		position = 25
	)
	default boolean pathToAmmunitionCrate()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathToTetherPoint",
		name = "Path to tether point",
		description = "Draws a route to the nearest mast or totem when a colossal wave is incoming",
		section = targetPaths,
		position = 26
	)
	default boolean pathToTetherPoint()
	{
		return true;
	}

	@ConfigSection(
		name = "Colossal wave",
		description = "Configure colossal wave guidance",
		position = 26
	)
	String colossalWave = "colossalWave";

	@ConfigItem(
		keyName = "waveTimerMode",
		name = "Enable wave timer",
		description = "Shows a pie, tick, or second timer until the wave reaches the masts and totems",
		section = colossalWave,
		position = 27
	)
	default TemporossTimerMode waveTimerMode()
	{
		return TemporossTimerMode.PIE;
	}

	@Alpha
	@ConfigItem(
		keyName = "waveTimerColor",
		name = "Wave timer color",
		description = "Color of the wave timer while untethered",
		section = colossalWave,
		position = 28
	)
	default Color waveTimerColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "tetheredColor",
		name = "Tethered color",
		description = "Color of the wave timer while tethered",
		section = colossalWave,
		position = 29
	)
	default Color tetheredColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "waveNotification",
		name = "Colossal wave notification",
		description = "Notifies when a colossal wave closes in",
		section = colossalWave,
		position = 30
	)
	default Notification waveNotification()
	{
		return Notification.OFF;
	}

	@ConfigSection(
		name = "Infoboxes",
		description = "Configure inventory reminder icons",
		position = 31
	)
	String infoboxes = "infoboxes";

	@ConfigItem(
		keyName = "showWaterBucketInfoBox",
		name = "Show water bucket infobox",
		description = "Shows the number of buckets of water in the inventory",
		section = infoboxes,
		position = 32
	)
	default boolean showWaterBucketInfoBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHumidifyInfoBox",
		name = "Show Humidify infobox",
		description = "Shows the Humidify icon when two or fewer buckets of water remain",
		section = infoboxes,
		position = 33
	)
	default boolean showHumidifyInfoBox()
	{
		return true;
	}
}
