package com.temporossfirefighter;

import java.util.Arrays;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TemporossPathFireTest
{
	@Test
	public void requiresExactTileIntersection()
	{
		WorldArea fire = new WorldArea(new WorldPoint(100, 100, 0), 1, 1);

		assertTrue(TemporossFirefighterPlugin.areaIntersectsPath(fire, Arrays.asList(
			new WorldPoint(99, 100, 0), new WorldPoint(100, 100, 0))));
		assertFalse(TemporossFirefighterPlugin.areaIntersectsPath(fire, Arrays.asList(
			new WorldPoint(99, 100, 0), new WorldPoint(100, 101, 0))));
	}
}
