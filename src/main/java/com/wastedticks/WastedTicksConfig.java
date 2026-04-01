package com.wastedticks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("wastedticks")
public interface WastedTicksConfig extends Config
{
	@ConfigItem(
		keyName = "showSeconds",
		name = "Show as seconds",
		description = "Display wasted time as seconds instead of raw ticks (1 tick = 0.6s)"
	)
	default boolean showSeconds()
	{
		return false;
	}
}
