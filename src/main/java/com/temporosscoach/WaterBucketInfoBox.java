package com.temporosscoach;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

final class WaterBucketInfoBox extends InfoBox
{
	private static final Color LOW_COLOR = new Color(255, 82, 82);
	private static final Color MEDIUM_COLOR = Color.ORANGE;
	private static final Color HIGH_COLOR = new Color(0, 200, 83);

	private final TemporossState state;

	WaterBucketInfoBox(BufferedImage image, Plugin plugin, TemporossState state)
	{
		super(image, plugin);
		this.state = state;
	}

	@Override
	public String getText()
	{
		return Integer.toString(state.getWaterBuckets());
	}

	@Override
	public Color getTextColor()
	{
		return colorForCount(state.getWaterBuckets());
	}

	@Override
	public String getTooltip()
	{
		return "Buckets of water: " + state.getWaterBuckets();
	}

	static Color colorForCount(int count)
	{
		if (count >= 5)
		{
			return HIGH_COLOR;
		}
		if (count >= 3)
		{
			return MEDIUM_COLOR;
		}
		return LOW_COLOR;
	}
}
