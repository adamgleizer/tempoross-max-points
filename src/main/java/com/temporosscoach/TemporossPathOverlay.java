package com.temporosscoach;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class TemporossPathOverlay extends Overlay
{
	private final Client client;
	private final TemporossCoachPlugin plugin;
	private final TemporossCoachConfig config;

	@Inject
	private TemporossPathOverlay(
		Client client,
		TemporossCoachPlugin plugin,
		TemporossCoachConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldView worldView = client.getTopLevelWorldView();
		if (worldView == null)
		{
			return null;
		}
		Color configured = pathColor();
		Color fill = new Color(configured.getRed(), configured.getGreen(), configured.getBlue(), 80);
		for (WorldPoint point : plugin.getGuidancePath())
		{
			LocalPoint localPoint = LocalPoint.fromWorld(worldView, point);
			if (localPoint == null)
			{
				continue;
			}
			Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
			if (polygon != null)
			{
				graphics.setColor(fill);
				graphics.fill(polygon);
				graphics.setColor(configured);
				graphics.draw(polygon);
			}
		}
		return null;
	}

	private Color pathColor()
	{
		if (plugin.isWaveIncoming() && plugin.getWaveTetherTarget() != null)
		{
			return config.waveTimerColor();
		}
		TemporossInstruction instruction = plugin.getInstruction();
		if (instruction.getType() == TemporossInstruction.Type.GET_ROPE
			|| instruction.getType() == TemporossInstruction.Type.GET_HAMMER
			|| instruction.getType() == TemporossInstruction.Type.GET_BUCKETS)
		{
			return config.missingSupplyColor();
		}
		Object target = plugin.getGuidancePathTarget();
		if (target instanceof NPC
			&& plugin.getDoubleFishingSpots().contains(target))
		{
			return config.doubleFishingSpotColor();
		}
		if (instruction.getType() == TemporossInstruction.Type.FISH)
		{
			return config.fishingSpotColor();
		}
		if (instruction.getType() == TemporossInstruction.Type.COOK)
		{
			return config.cookingShrineColor();
		}
		if (instruction.getType() == TemporossInstruction.Type.ATTACK)
		{
			return config.spiritPoolColor();
		}
		return config.ammunitionCrateColor();
	}
}
