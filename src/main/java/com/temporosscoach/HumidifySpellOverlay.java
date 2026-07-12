package com.temporosscoach;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

final class HumidifySpellOverlay extends Overlay
{
	private static final BasicStroke BORDER_STROKE = new BasicStroke(2f);

	private final Client client;
	private final TemporossState state;
	private final TemporossCoachConfig config;

	@Inject
	private HumidifySpellOverlay(
		Client client,
		TemporossCoachPlugin plugin,
		TemporossCoachConfig config)
	{
		super(plugin);
		this.client = client;
		this.state = plugin.getState();
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightHumidifySpell()
			|| !state.isWaterBucketRatioLow())
		{
			return null;
		}

		Widget spellbook = client.getWidget(InterfaceID.MagicSpellbook.UNIVERSE);
		if (spellbook != null && !spellbook.isHidden())
		{
			renderWidget(graphics, client.getWidget(InterfaceID.MagicSpellbook.HUMIDIFY));
		}
		else
		{
			Widget magicTab = visibleWidget(
				InterfaceID.Toplevel.STONE6,
				InterfaceID.ToplevelOsrsStretch.STONE6,
				InterfaceID.ToplevelPreEoc.STONE6);
			renderWidget(graphics, magicTab);
		}
		return null;
	}

	private Widget visibleWidget(int... componentIds)
	{
		for (int componentId : componentIds)
		{
			Widget widget = client.getWidget(componentId);
			if (widget != null && !widget.isHidden())
			{
				return widget;
			}
		}
		return null;
	}

	private void renderWidget(Graphics2D graphics, Widget widget)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (bounds != null && !bounds.isEmpty())
		{
			Color color = config.humidifySpellColor();
			Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 60);
			OverlayUtil.renderPolygon(graphics, bounds, color, fill, BORDER_STROKE);
		}
	}
}
