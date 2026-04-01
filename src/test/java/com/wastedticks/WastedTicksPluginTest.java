package com.wastedticks;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WastedTicksPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WastedTicksPlugin.class);
		RuneLite.main(args);
	}
}
