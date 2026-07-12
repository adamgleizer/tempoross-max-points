package com.temporosscoach;

import java.awt.Color;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WaterBucketInfoBoxTest
{
	@Test
	public void colorsWaterBucketFillRatios()
	{
		assertEquals(new Color(255, 82, 82), WaterBucketInfoBox.colorForRatio(0, 0));
		assertEquals(new Color(255, 82, 82), WaterBucketInfoBox.colorForRatio(1, 6));
		assertEquals(Color.ORANGE, WaterBucketInfoBox.colorForRatio(2, 6));
		assertEquals(Color.ORANGE, WaterBucketInfoBox.colorForRatio(3, 6));
		assertEquals(new Color(0, 200, 83), WaterBucketInfoBox.colorForRatio(4, 6));
		assertEquals(new Color(0, 200, 83), WaterBucketInfoBox.colorForRatio(6, 6));
	}
}
