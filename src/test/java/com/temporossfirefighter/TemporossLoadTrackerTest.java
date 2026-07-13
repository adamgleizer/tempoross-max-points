package com.temporossfirefighter;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemporossLoadTrackerTest
{
	@Test
	public void keepsShortGraceWindowAfterConfirmedActivity()
	{
		TemporossLoadTracker tracker = new TemporossLoadTracker();

		assertFalse(tracker.update(10, false));
		assertTrue(tracker.update(10, true));
		assertTrue(tracker.update(12, false));
		assertFalse(tracker.update(13, false));
	}

	@Test
	public void chatActivityAlsoStartsWindow()
	{
		TemporossLoadTracker tracker = new TemporossLoadTracker();
		tracker.observeActivity(20);

		assertTrue(tracker.update(21, false));
		tracker.reset();
		assertFalse(tracker.update(21, false));
	}
}
