package com.temporossfirefighter;

import net.runelite.api.gameval.SpriteID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HumidifyStateTest
{
	@Test
	public void brightHumidifySpriteIsCastableEvenWhenTabIsClosed()
	{
		assertTrue(TemporossFirefighterPlugin.isHumidifyCastableState(
			SpriteID._2XLunarSpellsOn._20, false));
	}

	@Test
	public void darkFilteredOrMissingHumidifySpellIsNotCastable()
	{
		assertFalse(TemporossFirefighterPlugin.isHumidifyCastableState(
			SpriteID.LunarMagicOff.HUMIDIFY, false));
		assertFalse(TemporossFirefighterPlugin.isHumidifyCastableState(
			SpriteID._2XLunarSpellsOn._20, true));
		assertFalse(TemporossFirefighterPlugin.isHumidifyCastableState(null, false));
	}
}
