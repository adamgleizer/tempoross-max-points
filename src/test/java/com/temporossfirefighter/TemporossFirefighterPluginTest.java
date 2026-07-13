package com.temporossfirefighter;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TemporossFirefighterPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TemporossFirefighterPlugin.class);
		RuneLite.main(args);
	}
}
