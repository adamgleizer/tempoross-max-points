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

class TemporossStepOverlay extends OverlayPanel {
    private static final Color ACCENT_COLOR = new Color(0, 200, 220);
    private static final Color SATISFIED_COLOR = new Color(0, 200, 83);
    private static final Color UNSATISFIED_COLOR = new Color(255, 82, 82);
    private static final int DESCRIPTION_LINE_LENGTH = 70;

    private final TemporossCoachPlugin plugin;
    private final TemporossCoachConfig config;

    @Inject
    private TemporossStepOverlay(TemporossCoachPlugin plugin, TemporossCoachConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
    }

    void applyDefaultSize() {
        if (getPreferredSize() == null) {
            setPreferredSize(new Dimension(ComponentConstants.
                    STANDARD_WIDTH, 0));
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.getState().isInEncounter()) {
            return null;
        }

        TemporossInstruction instruction = plugin.getInstruction();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(instruction.getTitle())
			.color(ACCENT_COLOR)
			.build());
		boolean hasProgress = instruction.getType() == TemporossInstruction.Type.GET_ROPE
                || instruction.getType() == TemporossInstruction.Type.GET_HAMMER
                || instruction.getType() == TemporossInstruction.Type.GET_BUCKETS
			|| instruction.getProgressLabel() != null;
		if (instruction.getType() == TemporossInstruction.Type.GET_ROPE
                || instruction.getType() == TemporossInstruction.Type.GET_HAMMER
                || instruction.getType() == TemporossInstruction.Type.GET_BUCKETS) {
            TemporossState state = plugin.getState();
            addRequirement("Rope", state.getRope() >= 1,
                    state.getRope() >= 1 ? "Ready" : "Missing");
            addRequirement("Hammer", state.getHammer() >= 1,
                    state.getHammer() >= 1 ? "Ready" : "Missing");
            addRequirement(
                    "Free slots",
                    state.getFreeInventorySlots() <= 19,
                    state.getFreeInventorySlots() + " / 19");
        } else if (instruction.getProgressLabel() != null) {
            boolean satisfied = Boolean.TRUE.equals(instruction.getProgressSatisfied());
            Color requirementColor = satisfied ? SATISFIED_COLOR : UNSATISFIED_COLOR;
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(instruction.getProgressLabel())
                    .leftColor(Color.WHITE)
                    .right(instruction.getProgressText())
                    .rightColor(requirementColor)
                    .build());
        }
		if (instruction.getStatusLabel() != null) {
			addRequirement(instruction.getStatusLabel(),
				Boolean.TRUE.equals(instruction.getStatusSatisfied()), instruction.getStatusText());
		}

		if (hasProgress && instruction.getDescription() != null
			&& !instruction.getDescription().isEmpty()) {
			panelComponent.getChildren().add(LineComponent.builder()
				.left(" ")
				.build());
		}
        addWrappedDescription(instruction.getDescription());

        return super.render(graphics);
    }

    private void addRequirement(String label, boolean satisfied, String value) {
        Color color = satisfied ? SATISFIED_COLOR : UNSATISFIED_COLOR;
        panelComponent.getChildren().add(LineComponent.builder()
                .left(label)
                .leftColor(Color.WHITE)
                .right(value)
                .rightColor(color)
                .build());
    }

    private void addWrappedDescription(String description) {
        StringBuilder line = new StringBuilder();
        for (String word : description.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > DESCRIPTION_LINE_LENGTH) {
                addDescriptionLine(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            addDescriptionLine(line.toString());
        }
    }

    private void addDescriptionLine(String text) {
        panelComponent.getChildren().add(LineComponent.builder()
                .left(text)
                .leftColor(Color.WHITE)
                .build());
    }
}
