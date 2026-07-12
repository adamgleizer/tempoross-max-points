package com.temporosscoach;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TemporossObjectOverlayTest
{
	@Test
	public void formatsSecondsAndTicksLikeReferencePlugin()
	{
		assertEquals("7.8", TemporossObjectOverlay.formatTimer(TemporossTimerMode.SECONDS, 7_800));
		assertEquals("13", TemporossObjectOverlay.formatTimer(TemporossTimerMode.TICKS, 7_800));
		assertEquals("0", TemporossObjectOverlay.formatTimer(TemporossTimerMode.TICKS, 599));
	}
}
