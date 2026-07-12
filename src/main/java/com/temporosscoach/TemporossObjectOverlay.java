package com.temporosscoach;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.ProgressPieComponent;
import net.runelite.client.ui.overlay.components.TextComponent;

class TemporossObjectOverlay extends Overlay
{
	private static final int PIE_DIAMETER = 20;
	private final TemporossCoachPlugin plugin;
	private final TemporossCoachConfig config;
	private final Client client;
	private final TextComponent timerTextComponent = new TextComponent();

	@Inject
	private TemporossObjectOverlay(TemporossCoachPlugin plugin, TemporossCoachConfig config, Client client)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.fireTimerMode() != TemporossTimerMode.OFF)
		{
			renderFires(graphics);
		}
		if (plugin.getInstruction().getType() == TemporossInstruction.Type.DOUSE_FIRE
			&& config.highlightTargetShipFire())
		{
			renderTargetShipFires(graphics);
		}
		if (config.highlightPathFires())
		{
			renderFireClickboxes(graphics, plugin.getPathFires(), config.pathFireColor());
		}
		if (plugin.getInstruction().getType() == TemporossInstruction.Type.DOUSE_ALL_FIRES
			&& config.highlightFirefightingTargets())
		{
			renderActiveFireClickboxes(graphics);
		}
		if (config.highlightDamagedTetherPoints())
		{
			renderDamagedTetherPoints(graphics);
		}
		renderNearestFishingSpot(graphics);

		TemporossInstruction.Type instructionType = plugin.getInstruction().getType();
		if (instructionType == TemporossInstruction.Type.GET_ROPE
			&& config.highlightMissingSupplies())
		{
			renderNearestSupply(graphics, plugin.getRopeSupplies());
		}
		else if (instructionType == TemporossInstruction.Type.GET_HAMMER
			&& config.highlightMissingSupplies())
		{
			renderNearestSupply(graphics, plugin.getHammerSupplies());
		}
		else if ((instructionType == TemporossInstruction.Type.GET_BUCKETS
			|| instructionType == TemporossInstruction.Type.FILL_BUCKETS)
			&& config.highlightMissingSupplies())
		{
			renderNearestSupply(graphics, plugin.getBucketSupplies());
		}
		else if (instructionType == TemporossInstruction.Type.COOK && config.highlightCookingShrines())
		{
			for (GameObject shrine : plugin.getCookingShrines())
			{
				Shape shape = shrine.getConvexHull();
				if (shape == null)
				{
					shape = shrine.getClickbox();
				}
				if (shape == null)
				{
					shape = shrine.getCanvasTilePoly();
				}
				renderHull(graphics, shape, config.cookingShrineColor());
			}
		}
		else if (instructionType == TemporossInstruction.Type.FISH)
		{
			renderDoubleFishingSpots(graphics);
		}
		else if (instructionType == TemporossInstruction.Type.LOAD
			&& config.highlightAmmunitionCrates())
		{
			renderNearestAmmunitionCrate(graphics);
		}
		else if (instructionType == TemporossInstruction.Type.ATTACK
			&& config.highlightSpiritPool())
		{
			renderTargetSpiritPool(graphics);
		}
		if (plugin.isWaveIncoming())
		{
			renderWaveTarget(graphics);
		}

		return null;
	}

	private void renderTargetSpiritPool(Graphics2D graphics)
	{
		NPC pool = plugin.getTargetSpiritPool();
		if (pool == null)
		{
			return;
		}

		Shape shape = pool.getConvexHull();
		if (shape == null)
		{
			shape = pool.getCanvasTilePoly();
		}
		renderHull(graphics, shape, config.spiritPoolColor());
	}

	private void renderNearestSupply(Graphics2D graphics, Iterable<GameObject> supplies)
	{
		GameObject nearest = nearestGameObject(supplies);
		if (nearest == null)
		{
			return;
		}

		Shape hull = nearest.getConvexHull();
		if (hull == null)
		{
			hull = nearest.getCanvasTilePoly();
		}
		renderHull(graphics, hull, config.missingSupplyColor());
	}

	private void renderFires(Graphics2D graphics)
	{
		renderFires(graphics, plugin.getFireObjects().keySet());
	}

	private void renderFires(Graphics2D graphics, Iterable<GameObject> fires)
	{
		Instant now = Instant.now();
		Map<GameObject, TemporossTimedObject> timedFires = plugin.getFireObjects();
		for (GameObject fire : fires)
		{
			TemporossTimedObject timedObject = timedFires.get(fire);
			if (timedObject == null)
			{
				continue;
			}
			renderTile(graphics, fire, config.fireColor());

			long duration = timedObject.getDurationMillis();
			long elapsedInCycle = Math.floorMod(
				now.toEpochMilli() - timedObject.getStartTime().toEpochMilli(), duration);
			long timeUntilCycle = Math.floorMod(
				timedObject.getStartTime().toEpochMilli() + duration - now.toEpochMilli(), duration);
			renderTimer(graphics, fire, config.fireTimerMode(), config.fireColor(),
				timeUntilCycle, now.minusMillis(elapsedInCycle), duration, now);
		}
	}

	private void renderTargetShipFires(Graphics2D graphics)
	{
		renderFireClickboxes(graphics, plugin.getShipFires(), config.targetShipFireColor());
	}

	private static void renderFireClickboxes(
		Graphics2D graphics,
		Iterable<GameObject> fires,
		Color color)
	{
		for (GameObject fire : fires)
		{
			renderFireClickbox(graphics, fire, color);
		}
	}

	private void renderActiveFireClickboxes(Graphics2D graphics)
	{
		for (GameObject fire : plugin.getFirefightingTargets())
		{
			renderFireClickbox(graphics, fire, config.firefightingTargetColor());
		}
	}

	private static void renderFireClickbox(Graphics2D graphics, GameObject fire, Color color)
	{
		Shape shape = fire.getClickbox();
		if (shape == null)
		{
			shape = fire.getConvexHull();
		}
		if (shape == null)
		{
			shape = fire.getCanvasTilePoly();
		}
		renderHull(graphics, shape, color);
	}

	private void renderDamagedTetherPoints(Graphics2D graphics)
	{
		Object waveTarget = plugin.isWaveIncoming() ? plugin.getWaveTetherTarget() : null;
		for (GameObject tetherPoint : plugin.getTetherPoints())
		{
			if (plugin.isWaveIncoming() && tetherPoint != waveTarget)
			{
				continue;
			}
			if (isDamagedTetherPoint(tetherPoint.getId()))
			{
				renderTile(graphics, tetherPoint, config.damagedTetherPointColor());
				renderHull(graphics, tetherPoint.getConvexHull(), config.damagedTetherPointColor());
			}
		}
	}

	private void renderWaveTarget(Graphics2D graphics)
	{
		Instant impactTime = plugin.getWaveImpactTime();
		TemporossTimerMode timerMode = config.waveTimerMode();
		Object target = plugin.getWaveTetherTarget();
		if (!(target instanceof GameObject))
		{
			return;
		}
		GameObject tetherPoint = (GameObject) target;
		if (plugin.getTetherPoints().contains(tetherPoint))
		{
			Color color = isDamagedTetherPoint(tetherPoint.getId())
				&& config.highlightDamagedTetherPoints()
				? config.damagedTetherPointColor()
				: plugin.getState().isTethered() ? config.tetheredColor() : config.waveTimerColor();
			Shape shape = tetherPoint.getConvexHull();
			if (shape == null)
			{
				shape = tetherPoint.getCanvasTilePoly();
			}
			renderHull(graphics, shape, color);

			if (timerMode != TemporossTimerMode.OFF && impactTime != null)
			{
				Instant now = Instant.now();
				long remainingMillis = Math.max(0, impactTime.toEpochMilli() - now.toEpochMilli());
				Instant startTime = impactTime.minusMillis(TemporossCoachPlugin.WAVE_IMPACT_MILLIS);
				renderTimer(graphics, tetherPoint, timerMode, color, remainingMillis,
					startTime, TemporossCoachPlugin.WAVE_IMPACT_MILLIS, now);
			}
		}
	}

	private static boolean isDamagedTetherPoint(int objectId)
	{
		return objectId == ObjectID.TEMPOROSS_MAST_BOTTOM_WEST_BROKEN
			|| objectId == ObjectID.TEMPOROSS_MAST_BOTTOM_EAST_BROKEN
			|| objectId == ObjectID.TEMPOROSS_TOTEM_NORTH_BROKEN
			|| objectId == ObjectID.TEMPOROSS_TOTEM_SOUTH_BROKEN;
	}

	private static void renderTile(Graphics2D graphics, GameObject gameObject, Color color)
	{
		Shape tile = gameObject.getCanvasTilePoly();
		if (tile != null)
		{
			OverlayUtil.renderPolygon(graphics, tile, color);
		}
	}

	private void renderTimer(
		Graphics2D graphics,
		GameObject gameObject,
		TemporossTimerMode mode,
		Color color,
		long remainingMillis,
		Instant startTime,
		long durationMillis,
		Instant now)
	{
		Color timerColor = opaque(color);
		if (gameObject.getCanvasLocation() == null)
		{
			return;
		}

		if (mode == TemporossTimerMode.PIE)
		{
			float progress = Math.max(0f, Math.min(1f,
				(now.toEpochMilli() - startTime.toEpochMilli()) / (float) durationMillis));
			ProgressPieComponent pie = new ProgressPieComponent();
			pie.setBorderColor(timerColor);
			pie.setFill(timerColor);
			pie.setProgress(progress);
			pie.setDiameter(PIE_DIAMETER);
			pie.setPosition(gameObject.getCanvasLocation());
			pie.render(graphics);
			return;
		}

		String timerText = formatTimer(mode, remainingMillis);
		timerTextComponent.setText(timerText);
		timerTextComponent.setColor(timerColor);
		timerTextComponent.setPosition(new java.awt.Point(
			gameObject.getCanvasLocation().getX(), gameObject.getCanvasLocation().getY()));
		timerTextComponent.render(graphics);
	}

	private static Color opaque(Color color)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue());
	}

	static String formatTimer(TemporossTimerMode mode, long remainingMillis)
	{
		if (mode == TemporossTimerMode.SECONDS)
		{
			return String.format(Locale.ROOT, "%.1f", remainingMillis / 1_000f);
		}
		return Long.toString(remainingMillis / 600);
	}

	private void renderNearestAmmunitionCrate(Graphics2D graphics)
	{
		NPC nearest = nearestNpc(plugin.getAmmunitionCrates());
		if (nearest == null)
		{
			return;
		}

		Shape hull = nearest.getConvexHull();
		if (hull == null)
		{
			hull = nearest.getCanvasTilePoly();
		}
		renderHull(graphics, hull, config.ammunitionCrateColor());
	}

	private void renderNearestFishingSpot(Graphics2D graphics)
	{
		if (!config.highlightFishingSpots())
		{
			return;
		}
		NPC selected = plugin.getNormalFishingSpotTarget();
		NPC nearest = selected != null && plugin.getFishingSpots().contains(selected)
			? selected
			: nearestNpc(plugin.getFishingSpots());
		if (nearest != null)
		{
			renderHull(graphics, nearest.getConvexHull(), config.fishingSpotColor());
		}
	}

	private void renderDoubleFishingSpots(Graphics2D graphics)
	{
		if (config.highlightDoubleFishingSpots())
		{
			for (NPC doubleFishingSpot : plugin.getDoubleFishingSpots())
			{
				renderHull(graphics, doubleFishingSpot.getConvexHull(), config.doubleFishingSpotColor());
			}
		}
	}

	private NPC nearestNpc(Iterable<NPC> npcs)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		NPC nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (NPC npc : npcs)
		{
			int distance = player.getWorldLocation().distanceTo(npc.getWorldLocation());
			if (distance < nearestDistance)
			{
				nearest = npc;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private GameObject nearestGameObject(Iterable<GameObject> gameObjects)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		GameObject nearest = null;
		int nearestDistance = Integer.MAX_VALUE;
		for (GameObject gameObject : gameObjects)
		{
			int distance = player.getWorldLocation().distanceTo(gameObject.getWorldLocation());
			if (distance < nearestDistance)
			{
				nearest = gameObject;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	private static void renderHull(Graphics2D graphics, Shape hull, Color color)
	{
		if (hull != null)
		{
			OverlayUtil.renderPolygon(graphics, hull, color);
		}
	}
}
