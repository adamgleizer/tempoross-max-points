package com.temporossfirefighter;

import java.time.Instant;

final class TemporossTimedObject
{
	private final Instant startTime;
	private final int durationMillis;

	TemporossTimedObject(Instant startTime, int durationMillis)
	{
		this.startTime = startTime;
		this.durationMillis = durationMillis;
	}

	Instant getStartTime()
	{
		return startTime;
	}

	int getDurationMillis()
	{
		return durationMillis;
	}
}
