package com.temporosscoach;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TemporossHudReaderTest
{
	@Test
	public void parsesAndClampsPercentText()
	{
		assertEquals(Integer.valueOf(93), TemporossHudReader.parsePercent("Storm: 93%"));
		assertEquals(Integer.valueOf(100), TemporossHudReader.parsePercent("125%"));
		assertNull(TemporossHudReader.parsePercent("Storm unavailable"));
		assertNull(TemporossHudReader.parsePercent(null));
	}

	@Test
	public void calculatesAndClampsBarPercent()
	{
		assertEquals(Integer.valueOf(50), TemporossHudReader.calculateBarPercent(40, 80));
		assertEquals(Integer.valueOf(100), TemporossHudReader.calculateBarPercent(90, 80));
		assertEquals(Integer.valueOf(0), TemporossHudReader.calculateBarPercent(-1, 80));
		assertNull(TemporossHudReader.calculateBarPercent(40, 0));
	}
}
