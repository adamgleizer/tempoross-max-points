package com.temporosscoach;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TemporossCoachPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(TemporossCoachPlugin.class);
		RuneLite.main(args);
	}
}
