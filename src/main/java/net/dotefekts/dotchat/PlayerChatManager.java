package net.dotefekts.dotchat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import net.dotefekts.dotchat.Format.ChatType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.chat.ComponentSerializer;

public class PlayerChatManager {
	private ProtocolManager protocolManager;
	private ChatManager chatManager;
	private Format formatting;
	private Player player;
	private ChatChannel activeChannel;
	private ChatChannel lastAvailableChatChannel;
	private ChatChannel activeChatChannel;
	private SortedMap<ChatChannel, Queue<PacketContainer>> channelMessages;
	private HashMap<ChatChannel, Integer> unreadMessages;
	
	public PlayerChatManager(ProtocolManager protocolManager, ChatManager chatManager, Format formatting, Player player, ChatChannel defaultChannel, ChatChannel defaultChatChannel, List<ChatChannel> channels) {
		this.protocolManager = protocolManager;
		this.chatManager = chatManager;
		this.formatting = formatting;
		this.player = player;
		this.activeChannel = defaultChannel;
		this.activeChatChannel = defaultChatChannel;
		this.lastAvailableChatChannel = defaultChatChannel;
		this.channelMessages = new TreeMap<ChatChannel, Queue<PacketContainer>>((a, b) -> a.getOrder() - b.getOrder());
		this.unreadMessages = new HashMap<ChatChannel, Integer>();
		
		for(ChatChannel channel : channels) {
			if(channel.canJoin(player)) {				
				channelMessages.put(channel, channel.getHistory());
				unreadMessages.put(channel, 0);
			}
		}
		
		if(!channelMessages.containsKey(defaultChannel)) {
			channelMessages.put(defaultChannel, defaultChannel.getHistory());
			unreadMessages.put(defaultChannel, 0);
		}
	}
	
	public void sendAllMessage(PacketContainer message) {
		for(Queue<PacketContainer> channelQueue : channelMessages.values()) {
			channelQueue.add(message);
			while(channelQueue.size() > ChatChannel.CHAT_LIMIT) {
				channelQueue.poll();
			}
		}

		sendMessages();
	}
	
	public void sendMessage(ChatChannel sourceChannel, PacketContainer message) {
		if(channelMessages.containsKey(sourceChannel)) {
			Queue<PacketContainer> channelQueue = channelMessages.get(sourceChannel);
			channelQueue.add(message);
			while(channelQueue.size() > ChatChannel.CHAT_LIMIT) {
				channelQueue.poll();
			}

			if(chatManager.getMultiChannel() != null) {
				PacketContainer prefixedMessage = ChatUtilities.addChannelPrefix(message, formatting.getAllSourceName(sourceChannel.getDisplayName(false)));
				
				Queue<PacketContainer> multiChannelQueue = channelMessages.get(chatManager.getMultiChannel());
				multiChannelQueue.add(prefixedMessage);
				while(multiChannelQueue.size() > ChatChannel.CHAT_LIMIT) {
					multiChannelQueue.poll();
				}
			}
			
			if(sourceChannel != activeChannel && !activeChannel.isMultiChannel()) {
				unreadMessages.put(sourceChannel, unreadMessages.get(sourceChannel) + 1);
			}
			
			sendMessages();
		}
	}
	
	public ChatChannel getActiveChannel() {
		return activeChannel;
	}
	
	public ChatChannel getActiveChatChannel() {
		return activeChatChannel;
	}
	
	public boolean joinChannel(ChatChannel channel) {
		if(channel.canJoin(player)) {
			channelMessages.put(channel, channel.getHistory());
			unreadMessages.put(channel, 0);
			
			switchChannel(channel);
			
			return true;
		}
		
		return false;
	}

	public boolean switchChannel(ChatChannel channel) {
		if(channelMessages.containsKey(channel)) {
			activeChannel = channel;
			
			if(activeChannel.isMultiChannel()) {
				unreadMessages.replaceAll((c, i) -> 0);
				activeChatChannel = lastAvailableChatChannel;
			} else {
				activeChatChannel = channel;
				unreadMessages.put(channel, 0);
				
				if(activeChatChannel.canTalk())
					lastAvailableChatChannel = activeChatChannel;
			}
			
			sendMessages();
			
			return true;
		} else {
			return false;
		}
	}
	
	private void sendMessages() {
		for(PacketContainer packet : channelMessages.get(activeChannel)) {
			try {
				protocolManager.sendServerPacket(player, packet, false);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		try {
			if(formatting.getChatSeparator() != null)
				protocolManager.sendServerPacket(player, ChatUtilities.buildChatPacket(ComponentSerializer.toString(formatting.getChatSeparator()), true, true), false);
			protocolManager.sendServerPacket(player, ChatUtilities.buildChatPacket(buildTabJson(), true, true), false);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	private String buildTabJson() {
		List<BaseComponent> components = new ArrayList<BaseComponent>();
		
		boolean first = true;
		for(ChatChannel channel : channelMessages.keySet()) {
			if(!first) {
				components.addAll(Arrays.asList(formatting.getTabSeparator()));
			}
			
			Format.ChatType type;
			if(activeChannel == channel) {
				type = ChatType.ACTIVE;
			} else if(activeChatChannel == channel) {
				type = ChatType.CHATTING;
			} else {
				type = ChatType.INACTIVE;
			}
			
			List<BaseComponent> channelComponents = new ArrayList<BaseComponent>(Arrays.asList(formatting.getTabName(channel, type, unreadMessages.get(channel))));
			for(BaseComponent c : channelComponents)
				c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/ch " + channel.getName()));
			
			components.addAll(channelComponents);
			
			first = false;
		}
		
		return ComponentSerializer.toString(components.toArray(new BaseComponent[components.size()]));
	}
}
