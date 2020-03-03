package net.dotefekts.dotchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

public class ChatManager {
	private static final String[] BLOCKED_CHANNEL_NAMES = { "all", "party" };
	
	private ProtocolManager protocolManager;
	private Format formatting;
	private int partyOrder = -1;
	private int pmOrder = -1;
	private boolean sendPartyHistory;
	private boolean sendPmHistory;
	private ChatChannel multiChannel = null;
	private ChatChannel defaultChannel = null;
	private ChatChannel systemMessageChannel = null;
	private HashMap<String, ChatChannel> channels;
	private HashMap<UUID, PlayerChatManager> playerManagers;
	
	public ChatManager(DotChat plugin, ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
		this.channels = new HashMap<String, ChatChannel>();
		this.playerManagers = new HashMap<UUID, PlayerChatManager>();
		
		int channelOrder = 0;
		
		FileConfiguration config = plugin.getConfig();
		this.formatting = new Format(config.getConfigurationSection("format"));
				
		if(config.getBoolean("enable-all", true)) {
			multiChannel = new ChatChannel(
					"all", 
					channelOrder++,
					this.formatting.getAllTabName(), 
					this.formatting.getAllTabNameActive(), 
					true, 
					true, 
					true,
					false,
					false,
					true);
			channels.put("all", multiChannel);
		}
		
		if(config.getBoolean("enable-party", true)) {
			partyOrder = channelOrder++;
		}
		
		sendPartyHistory = config.getBoolean("party-history", true);
		sendPmHistory = config.getBoolean("pm-history", true);
		
		ConfigurationSection channelsSection = config.getConfigurationSection("channels");
		if(channelsSection == null) {
			channels.put("global", new ChatChannel(
					"global", 
					channelOrder++,
					"§7Global",
					"§7§lGlobal",
					false, 
					true, 
					true,
					false,
					true,
					true));
			
		} else {
			for(String channel : channelsSection.getKeys(false)) {
				if(!Arrays.asList(BLOCKED_CHANNEL_NAMES).contains(channel.toLowerCase())) {
					ConfigurationSection channelSection = channelsSection.getConfigurationSection(channel);
					String displayName = channelSection.getString("name", "§7" + channel.toLowerCase());
					channels.put(channel.toLowerCase(), new ChatChannel(
							channel.toLowerCase(), 
							channelOrder++,
							displayName, 
							channelSection.getString("name-active", displayName), 
							false, 
							channelSection.getBoolean("public", true), 
							channelSection.getBoolean("auto-join", true),
							channelSection.getBoolean("can-leave", true),
							channelSection.getBoolean("can-talk", true),
							channelSection.getBoolean("history", true)));
				} else {
					plugin.getLogger().warning("Attempted to register a restricted channel name (" + channel + ").");
				}
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
		
		List<ChatChannel> sortedList = new ArrayList<ChatChannel>(channels.values());
		sortedList.sort((a, b) -> a.getOrder() - b.getOrder());
		
		for(ChatChannel channel : sortedList) {
			if(!channel.isMultiChannel() && channel.isPublic() && channel.isAutoJoin() && !channel.canLeave()) {
				defaultChannel = channel;
				break;
			}
		}
		
		if(defaultChannel == null) {
			plugin.getLogger().warning("You must have at least 1 channel that is public, auto-joined, and cannot be left. " + (channels.containsKey("global") ? "Replaced" : "Added") + " global channel with these properties.");
			channels.put("global", new ChatChannel(
					"global",
					channelOrder++,
					"§7Global",
					"§7§lGlobal",
					false,
					true,
					true,
					false,
					true,
					true));
		}
		
		pmOrder = channelOrder;
	}
	
	public int getPartyOrder() {
		return partyOrder;
	}

	public boolean sendPartyHistory() {
		return sendPartyHistory;
	}
	
	public int getPmOrder() {
		return pmOrder;
	}

	public boolean sendPmHistory() {
		return sendPmHistory;
	}

	public List<ChatChannel> getChannels() {
		return new ArrayList<ChatChannel>(channels.values());
	}

	public ChatChannel getChannel(String channelName) {
		return channels.get(channelName.toLowerCase());
	}
	
	public ChatChannel getMultiChannel() {
		return multiChannel;
	}

	public ChatChannel getSystemMessageChannel() {
		return systemMessageChannel;
	}

	public ChatChannel getDefaultChannel() {
		return multiChannel != null ? multiChannel : defaultChannel;
		
	}

	public ChatChannel getDefaultChatChannel() {
		return defaultChannel;
		
	}

	public PlayerChatManager getPlayerManager(Player player) {
		PlayerChatManager manager = playerManagers.get(player.getUniqueId());
		
		if(manager == null) {
			List<ChatChannel> channels = getDefaultPlayerChannels(player);
			manager = new PlayerChatManager(protocolManager, this, pmOrder, formatting, player, multiChannel != null ? multiChannel : defaultChannel, defaultChannel, channels);
			playerManagers.put(player.getUniqueId(), manager);
		}
		
		return manager;
	}

	public PlayerChatManager getPlayerManager(UUID uuid) {
		PlayerChatManager manager = playerManagers.get(uuid);
		return manager;
	}

	public void destroyPlayerManager(Player player) {
		PlayerChatManager manager = playerManagers.get(player.getUniqueId());
		if(manager != null) {
			manager.destroy();
			playerManagers.remove(player.getUniqueId());
		}
	}
	
	private List<ChatChannel> getDefaultPlayerChannels(Player player) {
		List<ChatChannel> channels = new ArrayList<ChatChannel>();
		for(ChatChannel channel : this.channels.values()) {
			if(channel.isAutoJoin() && channel.canJoin(player))
				channels.add(channel);
		}
		
		return channels;
	}

	public void addMessageHistory(ChatChannel chatChannel, PacketContainer chatPacket) {
		chatChannel.addMessage(chatPacket);
	}
}
