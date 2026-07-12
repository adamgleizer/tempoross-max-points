package com.temporosscoach;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

class TemporossPreparationOverlay extends OverlayPanel
{
	private static final Color ACCENT_COLOR = new Color(0, 200, 220);
	private static final Color SATISFIED_COLOR = new Color(0, 200, 83);
	private static final Color UNSATISFIED_COLOR = new Color(255, 82, 82);
	private static final int REQUIRED_FREE_SLOTS = 19;

	private final TemporossCoachPlugin plugin;
	private final TemporossCoachConfig config;

	@Inject
	private TemporossPreparationOverlay(TemporossCoachPlugin plugin, TemporossCoachConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	void applyDefaultSize()
	{
		if (getPreferredSize() == null)
		{
			setPreferredSize(new Dimension(ComponentConstants.STANDARD_WIDTH, 0));
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		TemporossState state = plugin.getState();
		if (state.isInEncounter())
		{
			return null;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Checklist")
			.color(ACCENT_COLOR)
			.build());
		addRequirement("Rope / S.Angler", state.getRope() >= 1,
			state.getRope() >= 1 ? "Ready" : "Missing");
		addRequirement("Hammer", state.getHammer() >= 1,
			state.getHammer() >= 1 ? "Ready" : "Missing");
		addRequirement("Humidify", state.hasHumidifyRunes(),
			state.hasHumidifyRunes() ? "Ready" : "Missing");
		addRequirement("Harpoon", state.getHarpoons() >= 1, state.getHarpoons() >= 1 ? "Ready" : "Missing");
		addRequirement(
			"Free slots",
			state.getFreeInventorySlots() >= REQUIRED_FREE_SLOTS,
			state.getFreeInventorySlots() + " / " + REQUIRED_FREE_SLOTS + "+");

		return super.render(graphics);
	}

	private void addRequirement(String label, boolean satisfied, String value)
	{
		Color color = satisfied ? SATISFIED_COLOR : UNSATISFIED_COLOR;
		panelComponent.getChildren().add(LineComponent.builder()
			.left(label)
			.leftColor(Color.WHITE)
			.right(value)
			.rightColor(color)
			.build());
	}
}
