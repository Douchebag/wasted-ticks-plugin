package com.wastedticks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class WastedTicksPanel extends PluginPanel
{
	private final WastedTicksPlugin plugin;
	private final JComboBox<String> friendDropdown;
	private final JPanel listPanel;

	public WastedTicksPanel(WastedTicksPlugin plugin)
	{
		super(false);
		this.plugin = plugin;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Top section: dropdown + track button
		JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
		inputPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		friendDropdown = new JComboBox<>();
		friendDropdown.setToolTipText("Select a friend to track");

		JButton addButton = new JButton("Track");
		addButton.addActionListener(e -> addSelectedFriend());

		inputPanel.add(friendDropdown, BorderLayout.CENTER);
		inputPanel.add(addButton, BorderLayout.EAST);

		// Header
		JLabel header = new JLabel("Wasted Ticks Tracker");
		header.setForeground(Color.WHITE);
		header.setAlignmentX(CENTER_ALIGNMENT);
		header.setBorder(new EmptyBorder(0, 0, 8, 0));

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
		topPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		topPanel.add(header);
		topPanel.add(inputPanel);

		add(topPanel, BorderLayout.NORTH);

		// Friend list
		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

		add(listPanel, BorderLayout.CENTER);
	}

	private void addSelectedFriend()
	{
		String selected = (String) friendDropdown.getSelectedItem();
		if (selected != null && !selected.isEmpty())
		{
			plugin.addTrackedFriend(selected);
		}
	}

	public void rebuild()
	{
		SwingUtilities.invokeLater(() ->
		{
			// Refresh the dropdown with untracked friends
			String previousSelection = (String) friendDropdown.getSelectedItem();
			friendDropdown.removeAllItems();
			for (String name : plugin.getUntrackedFriends())
			{
				friendDropdown.addItem(name);
			}
			if (previousSelection != null)
			{
				friendDropdown.setSelectedItem(previousSelection);
			}

			listPanel.removeAll();

			Map<String, Long> tickData = plugin.getTickData();
			Map<String, Boolean> tracked = plugin.getTrackedFriends();
			Map<String, Boolean> onlineStatus = plugin.getOnlineStatus();

			// Sort by ticks descending
			List<String> names = new ArrayList<>(tracked.keySet());
			names.sort(Comparator.comparingLong(n -> -tickData.getOrDefault(n, 0L)));

			for (String name : names)
			{
				long ticks = tickData.getOrDefault(name, 0L);
				boolean online = onlineStatus.getOrDefault(name, false);
				listPanel.add(createFriendRow(name, ticks, online));
			}

			listPanel.revalidate();
			listPanel.repaint();
		});
	}

	private static final Color OFFLINE_RED = new Color(255, 80, 80);
	private static final Color ONLINE_GREEN = new Color(80, 220, 80);

	private JPanel createFriendRow(String name, long ticks, boolean online)
	{
		JPanel row = new JPanel(new BorderLayout(5, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(BorderFactory.createCompoundBorder(
			new EmptyBorder(2, 0, 2, 0),
			new EmptyBorder(6, 8, 6, 8)
		));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		// Left side: name and tick count
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
		infoPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JLabel nameLabel = new JLabel(capitalize(name));
		nameLabel.setForeground(Color.WHITE);

		JLabel tickLabel = new JLabel(formatTicks(ticks));
		tickLabel.setForeground(online ? ONLINE_GREEN : OFFLINE_RED);
		tickLabel.setFont(tickLabel.getFont().deriveFont(10f));

		infoPanel.add(nameLabel);
		infoPanel.add(tickLabel);

		// Right side: buttons
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
		buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		JButton resetButton = new JButton("R");
		resetButton.setToolTipText("Reset ticks for " + name);
		resetButton.setPreferredSize(new Dimension(30, 24));
		resetButton.addActionListener(e -> plugin.resetTicks(name));

		JButton removeButton = new JButton("X");
		removeButton.setToolTipText("Stop tracking " + name);
		removeButton.setPreferredSize(new Dimension(30, 24));
		removeButton.addActionListener(e -> plugin.removeTrackedFriend(name));

		buttonPanel.add(resetButton);
		buttonPanel.add(removeButton);

		row.add(infoPanel, BorderLayout.CENTER);
		row.add(buttonPanel, BorderLayout.EAST);

		return row;
	}

	private String formatTicks(long ticks)
	{
		// Show both ticks and a human-readable time
		long totalSeconds = (long) (ticks * 0.6);
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		if (hours > 0)
		{
			return String.format("%,d ticks (%dh %dm %ds)", ticks, hours, minutes, seconds);
		}
		else if (minutes > 0)
		{
			return String.format("%,d ticks (%dm %ds)", ticks, minutes, seconds);
		}
		else
		{
			return String.format("%,d ticks (%ds)", ticks, seconds);
		}
	}

	private static String capitalize(String s)
	{
		if (s == null || s.isEmpty())
		{
			return s;
		}
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
