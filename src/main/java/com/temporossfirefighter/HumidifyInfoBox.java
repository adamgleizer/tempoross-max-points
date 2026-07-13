package com.temporossfirefighter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

final class HumidifyInfoBox extends InfoBox
{
	HumidifyInfoBox(BufferedImage image, Plugin plugin)
	{
		super(image, plugin);
	}

	@Override
	public String getText()
	{
		return "";
	}

	@Override
	public Color getTextColor()
	{
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		return "Cast Humidify";
	}
}
