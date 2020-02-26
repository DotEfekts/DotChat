package net.dotefekts.dotchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolManager;

public class ChatManager {
	private DotChat plugin;
	private ProtocolManager protocolManager;
	private ChatChannel multiChannel = null;
	private ChatChannel defaultChannel = null;
	private ChatChannel systemMessageChannel = null;
	private HashMap<String, ChatChannel> channels;
	private HashMap<UUID, PlayerChatManager> playerManagers;
	
	public ChatManager(DotChat plugin, ProtocolManager protocolManager) {
		this.plugin = plugin;
		this.protocolManager = protocolManager;
		this.channels = new HashMap<String, ChatChannel>();
		this.playerManagers = new HashMap<UUID, PlayerChatManager>();
		
		int channelOrder = 0;
		
		FileConfiguration config = plugin.getConfig();
		if(config.getBoolean("enable-all", true)) {
			multiChannel = new ChatChannel(
					"all", 
					channelOrder++,
					"All", 
					true, 
					true, 
					true,
					false,
					true,
					true);
			channels.put("all", multiChannel);
		}
		
		ConfigurationSection channelsSection = config.getConfigurationSection("channels");
		if(channelsSection == null) {
			channels.put("global", new ChatChannel(
					"global", 
					channelOrder++,
					"Global", 
					false, 
					true, 
					true,
					false,
					true,
					true));
			
		} else {
			for(String channel : channelsSection.getKeys(false)) {
				ConfigurationSection channelSection = channelsSection.getConfigurationSection(channel);
				channels.put(channel, new ChatChannel(
						channel, 
						channelOrder++,
						channelSection.getString("name", channel), 
						false, 
						channelSection.getBoolean("public", true), 
						channelSection.getBoolean("auto-join", true),
						channelSection.getBoolean("can-leave", true),
						channelSection.getBoolean("can-talk", true),
						channelSection.getBoolean("history", true)));
			}
		}
		
		String sysMessage = config.getString("system-messages");
		if(!sysMessage.equals("all")) {
			for(ChatChannel channel : channels.values())
				if(channel.getName().equals(sysMessage))
					systemMessageChannel = channel;
			if(systemMessageChannel == null)
				plugin.getLogger().warning("Could not find system messages channel.");
			else if(!systemMessageChannel.isPublic() || !systemMessageChannel.isAutoJoin() || systemMessageChannel.canLeave()) {
				plugin.getLogger().warning("System messages channel must be public, auto-join, and non-leavable. Defaulting to all.");
				systemMessageChannel = null;
			}
		}
		
		for(ChatChannel channel : channels.values())
			if(!channel.isMultiChannel() && channel.isPublic() && channel.isAutoJoin() && !channel.canLeave()) {
				defaultChannel = channel;
				break;
			}
		
		if(defaultChannel == null) {
			plugin.getLogger().warning("You must have at least 1 channel that is public, auto-joined, and cannot be left. " + (channels.containsKey("global") ? "Replaced" : "Added") + " global channel with these properties.");
			channels.put("global", new ChatChannel(
					"global",
					channelOrder++,
					"Global",
					false,
					true,
					true,
					false,
					true,
					true));
		}
	}

	public ChatChannel getChannel(String channelName) {
		return channels.get(channelName);
	}
	
	public ChatChannel getMultiChannel() {
		return multiChannel;
	}

	public ChatChannel getSystemMessageChannel() {
		return systemMessageChannel;
	}

	public PlayerChatManager getPlayerManager(Player player) {
		PlayerChatManager manager = playerManagers.get(player.getUniqueId());
		
		if(manager == null) {
			List<ChatChannel> channels = getDefaultPlayerChannels(player);
			manager = new PlayerChatManager(protocolManager, this, player, multiChannel != null ? multiChannel : defaultChannel, defaultChannel, channels);
			playerManagers.put(player.getUniqueId(), manager);
		}
		
		return manager;
	}

	public PlayerChatManager getPlayerManager(UUID uuid) {
		PlayerChatManager manager = playerManagers.get(uuid);
		return manager;
	}

	public void destroyPlayerManager(Player player) {
		playerManagers.remove(player.getUniqueId());
	}
	
	private List<ChatChannel> getDefaultPlayerChannels(Player player) {
		List<ChatChannel> channels = new ArrayList<ChatChannel>();
		for(ChatChannel channel : this.channels.values()) {
			if(channel.isAutoJoin() && channel.canJoin(player))
				channels.add(channel);
		}
		
		return channels;
	}
}
