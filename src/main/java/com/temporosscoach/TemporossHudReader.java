package com.temporosscoach;

import java.awt.Rectangle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

final class TemporossHudReader
{
	private static final int ENCOUNTER_END_GRACE_TICKS = 3;
	private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,3})%");

	private final Client client;
	private int missingHudTicks;

	@Inject
	private TemporossHudReader(Client client)
	{
		this.client = client;
	}

	void update(TemporossState state)
	{
		boolean hudVisible = isVisible(client.getWidget(InterfaceID.TemporossHud.UNIVERSE))
			|| isVisible(client.getWidget(InterfaceID.TemporossHud.STATUS))
			|| isVisible(client.getWidget(InterfaceID.TemporossHud.ESSENCE_BAR_CONTAINER));

		if (hudVisible)
		{
			missingHudTicks = 0;
			state.setEncounterState(true,
				readPercent(
					InterfaceID.TemporossHud.ENERGY,
					InterfaceID.TemporossHud.ENERGY_BAR,
					InterfaceID.TemporossHud.ENERGY_BAR_BACK),
				readPercent(
					InterfaceID.TemporossHud.ESSENCE,
					InterfaceID.TemporossHud.ESSENCE_BAR,
					InterfaceID.TemporossHud.ESSENCE_BAR_BACK),
				readPercent(
					InterfaceID.TemporossHud.STORM_INTENSITY,
					InterfaceID.TemporossHud.STORM_INTENSITY_BAR,
					InterfaceID.TemporossHud.STORM_INTENSITY_BAR_BACK));
		}
		else if (!state.isInEncounter() || ++missingHudTicks >= ENCOUNTER_END_GRACE_TICKS)
		{
			state.clearEncounterState();
		}
	}

	void reset()
	{
		missingHudTicks = 0;
	}

	private Integer readPercent(int textComponentId, int barComponentId, int backComponentId)
	{
		Integer textPercent = findPercent(client.getWidget(textComponentId));
		if (textPercent != null)
		{
			return textPercent;
		}

		Widget bar = client.getWidget(barComponentId);
		Widget back = client.getWidget(backComponentId);
		if (!isVisible(bar) || !isVisible(back))
		{
			return null;
		}

		Rectangle barBounds = bar.getBounds();
		Rectangle backBounds = back.getBounds();
		if (barBounds == null || backBounds == null)
		{
			return null;
		}
		return calculateBarPercent(barBounds.width, backBounds.width);
	}

	private static Integer findPercent(Widget widget)
	{
		if (widget == null)
		{
			return null;
		}

		Integer percent = parsePercent(widget.getText());
		if (percent != null)
		{
			return percent;
		}

		Widget[] children = widget.getChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				Integer childPercent = findPercent(child);
				if (childPercent != null)
				{
					return childPercent;
				}
			}
		}
		return null;
	}

	static Integer parsePercent(String text)
	{
		Matcher matcher = PERCENT_PATTERN.matcher(text == null ? "" : text);
		if (!matcher.find())
		{
			return null;
		}
		return Math.min(100, Integer.parseInt(matcher.group(1)));
	}

	static Integer calculateBarPercent(int barWidth, int backWidth)
	{
		if (backWidth <= 0)
		{
			return null;
		}
		return Math.max(0, Math.min(100, Math.round(100f * barWidth / backWidth)));
	}

	private static boolean isVisible(Widget widget)
	{
		return widget != null && !widget.isHidden();
	}
}
