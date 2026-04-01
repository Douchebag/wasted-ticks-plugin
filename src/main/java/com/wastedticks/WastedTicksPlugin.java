package com.wastedticks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.GameState;
import net.runelite.api.NameableContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "Wasted Ticks Tracker",
	description = "Tracks how many game ticks your friends waste being offline",
	tags = {"friends", "ticks", "tracker", "offline"}
)
public class WastedTicksPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(WastedTicksPlugin.class);
	private static final String CONFIG_GROUP = "wastedticks";
	private static final String DATA_KEY = "tickdata";
	private static final String TRACKED_KEY = "trackedfriends";
	private static final Type TICK_DATA_TYPE = new TypeToken<Map<String, Long>>(){}.getType();
	private static final Type TRACKED_SET_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();

	@Inject
	private Client client;

	@Inject
	private WastedTicksConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Gson gson;

	private WastedTicksPanel panel;
	private NavigationButton navButton;

	/**
	 * Map of player name (lowercased) -> accumulated offline ticks.
	 */
	private Map<String, Long> tickData = new HashMap<>();

	/**
	 * Set of tracked friend names (lowercased) -> true. Using a map for easy JSON serialization.
	 */
	private Map<String, Boolean> trackedFriends = new HashMap<>();

	/**
	 * Map of player name (lowercased) -> whether they are currently online.
	 */
	private Map<String, Boolean> onlineStatus = new HashMap<>();

	@Override
	protected void startUp()
	{
		loadData();

		panel = new WastedTicksPanel(this);

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");

		navButton = NavigationButton.builder()
			.tooltip("Wasted Ticks Tracker")
			.icon(icon)
			.priority(10)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		panel.rebuild();
	}

	@Override
	protected void shutDown()
	{
		saveData();
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	@Provides
	WastedTicksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WastedTicksConfig.class);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		NameableContainer<Friend> friendContainer = client.getFriendContainer();
		if (friendContainer == null)
		{
			return;
		}

		for (String trackedName : trackedFriends.keySet())
		{
			boolean isOnline = false;

			for (Friend friend : friendContainer.getMembers())
			{
				if (sanitize(friend.getName()).equals(trackedName))
				{
					isOnline = friend.getWorld() > 0;
					break;
				}
			}

			onlineStatus.put(trackedName, isOnline);

			if (!isOnline)
			{
				tickData.merge(trackedName, 1L, Long::sum);
			}
		}

		if (panel != null)
		{
			panel.rebuild();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			saveData();
		}
	}

	private static String sanitize(String name)
	{
		return name.toLowerCase().replaceAll("[^a-z0-9]", " ").trim().replaceAll("\\s+", " ");
	}

	public void addTrackedFriend(String name)
	{
		String key = sanitize(name);
		if (key.isEmpty())
		{
			return;
		}

		trackedFriends.put(key, true);
		tickData.putIfAbsent(key, 0L);
		saveData();

		if (panel != null)
		{
			panel.rebuild();
		}
	}

	public void removeTrackedFriend(String name)
	{
		String key = sanitize(name);
		trackedFriends.remove(key);
		tickData.remove(key);
		saveData();

		if (panel != null)
		{
			panel.rebuild();
		}
	}

	public void resetTicks(String name)
	{
		String key = sanitize(name);
		tickData.put(key, 0L);
		saveData();

		if (panel != null)
		{
			panel.rebuild();
		}
	}

	public Map<String, Long> getTickData()
	{
		return tickData;
	}

	public Map<String, Boolean> getTrackedFriends()
	{
		return trackedFriends;
	}

	public Map<String, Boolean> getOnlineStatus()
	{
		return onlineStatus;
	}

	public List<String> getUntrackedFriends()
	{
		List<String> result = new ArrayList<>();
		NameableContainer<Friend> friendContainer = client.getFriendContainer();
		if (friendContainer == null)
		{
			return result;
		}

		for (Friend friend : friendContainer.getMembers())
		{
			String key = sanitize(friend.getName());
			if (!trackedFriends.containsKey(key))
			{
				result.add(friend.getName());
			}
		}

		result.sort(String.CASE_INSENSITIVE_ORDER);
		return result;
	}

	private void loadData()
	{
		String tickJson = configManager.getConfiguration(CONFIG_GROUP, DATA_KEY);
		if (tickJson != null)
		{
			try
			{
				tickData = gson.fromJson(tickJson, TICK_DATA_TYPE);
				if (tickData == null)
				{
					tickData = new HashMap<>();
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to load tick data", e);
				tickData = new HashMap<>();
			}
		}

		String trackedJson = configManager.getConfiguration(CONFIG_GROUP, TRACKED_KEY);
		if (trackedJson != null)
		{
			try
			{
				trackedFriends = gson.fromJson(trackedJson, TRACKED_SET_TYPE);
				if (trackedFriends == null)
				{
					trackedFriends = new HashMap<>();
				}
			}
			catch (Exception e)
			{
				log.warn("Failed to load tracked friends", e);
				trackedFriends = new HashMap<>();
			}
		}
	}

	private void saveData()
	{
		configManager.setConfiguration(CONFIG_GROUP, DATA_KEY, gson.toJson(tickData));
		configManager.setConfiguration(CONFIG_GROUP, TRACKED_KEY, gson.toJson(trackedFriends));
	}
}
