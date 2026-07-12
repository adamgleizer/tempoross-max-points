package com.temporosscoach;

import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WaterBucketInfoBoxTest
{
	@Test
	public void colorsWaterBucketRanges()
	{
		assertEquals(new Color(255, 82, 82), WaterBucketInfoBox.colorForCount(0));
		assertEquals(new Color(255, 82, 82), WaterBucketInfoBox.colorForCount(2));
		assertEquals(Color.ORANGE, WaterBucketInfoBox.colorForCount(3));
		assertEquals(Color.ORANGE, WaterBucketInfoBox.colorForCount(4));
		assertEquals(new Color(0, 200, 83), WaterBucketInfoBox.colorForCount(5));
		assertEquals(new Color(0, 200, 83), WaterBucketInfoBox.colorForCount(12));
	}
}
