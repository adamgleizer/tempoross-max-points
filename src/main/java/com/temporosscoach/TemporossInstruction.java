package com.temporosscoach;

import java.util.Optional;
import net.runelite.api.coords.WorldPoint;

public final class TemporossInstruction
{
	public enum Type
	{
		GENERAL,
		FISH,
		COOK,
		LOAD,
		ATTACK,
		SAFETY,
		DOUSE_FIRE,
		DOUSE_ALL_FIRES,
		GET_ROPE,
		GET_HAMMER,
		GET_BUCKETS
	}

	private final String title;
	private final String description;
	private final WorldPoint target;
	private final Type type;
	private final String progressLabel;
	private final String progressText;
	private final Boolean progressSatisfied;
	private final String statusLabel;
	private final String statusText;
	private final Boolean statusSatisfied;
	private final boolean doubleFishingSpotPreferred;

	public TemporossInstruction(String title, String description, WorldPoint target, Type type)
	{
		this(title, description, target, type, null, null, null, null, null, null, false);
	}

	public TemporossInstruction(
		String title,
		String description,
		WorldPoint target,
		Type type,
		String progressLabel,
		String progressText,
		Boolean progressSatisfied)
	{
		this(title, description, target, type, progressLabel, progressText,
			progressSatisfied, null, null, null, false);
	}

	public TemporossInstruction(
		String title,
		String description,
		WorldPoint target,
		Type type,
		String progressLabel,
		String progressText,
		Boolean progressSatisfied,
		String statusLabel,
		String statusText,
		Boolean statusSatisfied,
		boolean doubleFishingSpotPreferred)
	{
		this.title = title;
		this.description = description;
		this.target = target;
		this.type = type;
		this.progressLabel = progressLabel;
		this.progressText = progressText;
		this.progressSatisfied = progressSatisfied;
		this.statusLabel = statusLabel;
		this.statusText = statusText;
		this.statusSatisfied = statusSatisfied;
		this.doubleFishingSpotPreferred = doubleFishingSpotPreferred;
	}

	public TemporossInstruction(
			String title,
			WorldPoint target,
			Type type,
			String progressLabel,
			String progressText,
			Boolean progressSatisfied)
	{
		this(title, "", target, type, progressLabel, progressText,
			progressSatisfied, null, null, null, false);
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public Optional<WorldPoint> getTarget()
	{
		return Optional.ofNullable(target);
	}

	public Type getType()
	{
		return type;
	}

	public String getProgressLabel()
	{
		return progressLabel;
	}

	public String getProgressText()
	{
		return progressText;
	}

	public Boolean getProgressSatisfied()
	{
		return progressSatisfied;
	}

	public String getStatusLabel()
	{
		return statusLabel;
	}

	public String getStatusText()
	{
		return statusText;
	}

	public Boolean getStatusSatisfied()
	{
		return statusSatisfied;
	}

	public boolean isDoubleFishingSpotPreferred()
	{
		return doubleFishingSpotPreferred;
	}
}
