package com.temporosscoach;

final class TemporossLoadTracker
{
	private static final int ACTIVITY_GRACE_TICKS = 2;
	private int lastActivityTick = -1;

	void observeActivity(int tick)
	{
		lastActivityTick = tick;
	}

	boolean update(int tick, boolean interactingWithCrate)
	{
		if (interactingWithCrate)
		{
			observeActivity(tick);
		}
		return lastActivityTick >= 0
			&& tick - lastActivityTick <= ACTIVITY_GRACE_TICKS;
	}

	void reset()
	{
		lastActivityTick = -1;
	}
}
