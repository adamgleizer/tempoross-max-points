package com.temporosscoach;

import net.runelite.api.gameval.SpriteID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HumidifyStateTest
{
	@Test
	public void brightHumidifySpriteIsCastableEvenWhenTabIsClosed()
	{
		assertTrue(TemporossCoachPlugin.isHumidifyCastableState(
			SpriteID._2XLunarSpellsOn._20, false));
	}

	@Test
	public void darkFilteredOrMissingHumidifySpellIsNotCastable()
	{
		assertFalse(TemporossCoachPlugin.isHumidifyCastableState(
			SpriteID.LunarMagicOff.HUMIDIFY, false));
		assertFalse(TemporossCoachPlugin.isHumidifyCastableState(
			SpriteID._2XLunarSpellsOn._20, true));
		assertFalse(TemporossCoachPlugin.isHumidifyCastableState(null, false));
	}
}
