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
		name = "Target highlights",
		description = "Configure highlights for the current strategy target",
		position = 0
	)
	String targetHighlights = "targetHighlights";

	@ConfigSection(
		name = "Fire highlights",
		description = "Configure active fire and firefighting highlights",
		position = 20
	)
	String fireHighlights = "fireHighlights";

	@ConfigSection(
		name = "Target paths",
		description = "Configure routes to the current strategy target",
		position = 40
	)
	String targetPaths = "targetPaths";

	@ConfigSection(
		name = "Colossal wave",
		description = "Configure colossal wave guidance",
		position = 60
	)
	String colossalWave = "colossalWave";

	@ConfigSection(
		name = "Buckets and Humidify",
		description = "Configure bucket reminders and Humidify guidance",
		position = 80
	)
	String bucketsAndHumidify = "bucketsAndHumidify";

	@ConfigItem(
		keyName = "fireTimerMode",
		name = "Highlight fires",
		description = "Highlights fire tiles and optionally shows pie, tick, or second timers",
		section = fireHighlights,
		position = 21
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
		section = fireHighlights,
		position = 22
	)
	default Color fireColor()
	{
		return new Color(255, 165, 0, 80);
	}

	@ConfigItem(
		keyName = "highlightTargetShipFire",
		name = "Highlight target ship fire",
		description = "Adds a clickbox highlight to ship fires when dousing one is the current target",
		section = fireHighlights,
		position = 23
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
		section = fireHighlights,
		position = 24
	)
	default Color targetShipFireColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightPathFires",
		name = "Highlight fires in path",
		description = "Adds a clickbox highlight to active fires intersecting the current target path",
		section = fireHighlights,
		position = 25
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
		section = fireHighlights,
		position = 26
	)
	default Color pathFireColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightFirefightingTargets",
		name = "Highlight firefighting targets",
		description = "Adds clickbox highlights to the eight nearest active fires during final firefighting",
		section = fireHighlights,
		position = 27
	)
	default boolean highlightFirefightingTargets()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "firefightingTargetColor",
		name = "Firefighting target color",
		description = "Color of active fire clickbox highlights during final firefighting",
		section = fireHighlights,
		position = 28
	)
	default Color firefightingTargetColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightFishingSpots",
		name = "Highlight fishing spot",
		description = "Highlights the nearest normal harpoonfish spot while fishing",
		section = targetHighlights,
		position = 1
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
		section = targetHighlights,
		position = 2
	)
	default Color fishingSpotColor()
	{
		return new Color(0, 0, 139);
	}

	@ConfigItem(
		keyName = "highlightDoubleFishingSpots",
		name = "Highlight double spot",
		description = "Highlights double harpoonfish spots while fishing",
		section = targetHighlights,
		position = 3
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
		section = targetHighlights,
		position = 4
	)
	default Color doubleFishingSpotColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightCookingShrines",
		name = "Highlight cooking shrine",
		description = "Highlights cooking shrines while cooking fish",
		section = targetHighlights,
		position = 5
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
		section = targetHighlights,
		position = 6
	)
	default Color cookingShrineColor()
	{
		return new Color(0, 200, 220);
	}

	@ConfigItem(
		keyName = "highlightAmmunitionCrates",
		name = "Highlight ammunition crate",
		description = "Highlights the nearest ammunition crate while loading fish",
		section = targetHighlights,
		position = 7
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
		section = targetHighlights,
		position = 8
	)
	default Color ammunitionCrateColor()
	{
		return new Color(0, 200, 220);
	}

	@ConfigItem(
		keyName = "highlightDamagedTetherPoints",
		name = "Highlight damaged poles",
		description = "Highlights damaged masts and totem poles",
		section = colossalWave,
		position = 61
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
		section = colossalWave,
		position = 62
	)
	default Color damagedTetherPointColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "highlightMissingSupplies",
		name = "Highlight missing supplies",
		description = "Highlights rope, hammer, or bucket supplies when they are the current target",
		section = targetHighlights,
		position = 9
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
		section = targetHighlights,
		position = 10
	)
	default Color missingSupplyColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightSpiritPool",
		name = "Highlight spirit pool",
		description = "Highlights the spirit pool when attacking Tempoross is the current target",
		section = targetHighlights,
		position = 11
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
		section = targetHighlights,
		position = 12
	)
	default Color spiritPoolColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "highlightHumidifySpell",
		name = "Highlight Humidify",
		description = "Highlights Humidify when at most one-third of carried buckets contain water",
		section = bucketsAndHumidify,
		position = 81
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
		section = bucketsAndHumidify,
		position = 82
	)
	default Color humidifySpellColor()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "pathToFishingSpot",
		name = "Path to fishing spot",
		description = "Draws a route when a normal fishing spot is the current target",
		section = targetPaths,
		position = 41
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
		position = 42
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
		position = 43
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
		position = 44
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
		position = 45
	)
	default boolean pathToTetherPoint()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathToSpiritPool",
		name = "Path to spirit pool",
		description = "Draws a route to the strategy's target spirit pool when attacking Tempoross",
		section = targetPaths,
		position = 46
	)
	default boolean pathToSpiritPool()
	{
		return true;
	}

	@ConfigItem(
		keyName = "pathToMissingSupplies",
		name = "Path to missing supplies",
		description = "Draws a route to the required rope, hammer, or bucket supply",
		section = targetPaths,
		position = 47
	)
	default boolean pathToMissingSupplies()
	{
		return true;
	}

	@ConfigItem(
		keyName = "targetNotification",
		name = "Target change notification",
		description = "Notifies when the recommended action changes",
		section = targetPaths,
		position = 48
	)
	default Notification targetNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "waveTimerMode",
		name = "Enable wave timer",
		description = "Shows a pie, tick, or second timer until the wave reaches the masts and totems",
		section = colossalWave,
		position = 63
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
		position = 64
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
		position = 65
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
		position = 66
	)
	default Notification waveNotification()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "showWaterBucketInfoBox",
		name = "Show water bucket infobox",
		description = "Shows the number of buckets of water in the inventory",
		section = bucketsAndHumidify,
		position = 83
	)
	default boolean showWaterBucketInfoBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHumidifyInfoBox",
		name = "Show Humidify infobox",
		description = "Shows the Humidify icon when at most one-third of carried buckets contain water",
		section = bucketsAndHumidify,
		position = 84
	)
	default boolean showHumidifyInfoBox()
	{
		return true;
	}
}
