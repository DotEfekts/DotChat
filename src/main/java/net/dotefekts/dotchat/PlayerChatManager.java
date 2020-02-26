package net.dotefekts.dotchat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class PlayerChatManager {
	private static String SEPERATOR = "{\"text\":\"==================================================\",\"color\":\"gray\"}";
	
	private ProtocolManager protocolManager;
	private ChatManager chatManager;
	private Player player;
	private ChatChannel activeChannel;
	private ChatChannel activeChatChannel;
	private SortedMap<ChatChannel, Queue<PacketContainer>> channelMessages;
	private HashMap<ChatChannel, Integer> unreadMessages;
	
	public PlayerChatManager(ProtocolManager protocolManager, ChatManager chatManager, Player player, ChatChannel defaultChannel, ChatChannel defaultChatChannel, List<ChatChannel> channels) {
		this.protocolManager = protocolManager;
		this.chatManager = chatManager;
		this.player = player;
		this.activeChannel = defaultChannel;
		this.activeChatChannel = defaultChatChannel;
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
				PacketContainer multiMessage = message.shallowClone();
				
				WrappedChatComponent chatComponent = multiMessage.getChatComponents().readSafely(0);
				List<BaseComponent> messageComponents = new ArrayList<BaseComponent>(Arrays.asList(ComponentSerializer.parse(chatComponent.getJson())));
				messageComponents.add(0, new TextComponent("[" + sourceChannel.getDisplayName() + "] "));
				chatComponent.setJson(ComponentSerializer.toString(messageComponents.toArray(new BaseComponent[messageComponents.size()])));
				
				multiMessage.getChatComponents().write(0, chatComponent);
				
				Queue<PacketContainer> multiChannelQueue = channelMessages.get(chatManager.getMultiChannel());
				multiChannelQueue.add(multiMessage);
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
			} else {
				activeChatChannel = channel;
				unreadMessages.put(channel, 0);
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
			protocolManager.sendServerPacket(player, ChatUtilities.buildChatPacket(SEPERATOR, true, true), false);
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
				TextComponent seperatorComponent = new TextComponent("|");
				seperatorComponent.setColor(ChatColor.GRAY);
				components.add(seperatorComponent);
			}
			
			List<BaseComponent> channelComponents = new ArrayList<BaseComponent>(Arrays.asList(TextComponent.fromLegacyText(" " + org.bukkit.ChatColor.GRAY + channel.getDisplayName())));
			if(activeChannel == channel) {
				for(BaseComponent c : channelComponents)
					c.setBold(true);
			} else if(unreadMessages.get(channel) > 0) {
				TextComponent left = new TextComponent("(");
				TextComponent number = new TextComponent(unreadMessages.get(channel).toString());
				TextComponent right = new TextComponent(")");

				left.setColor(ChatColor.GRAY);
				number.setColor(ChatColor.YELLOW);
				right.setColor(ChatColor.GRAY);
				
				channelComponents.add(left);
				channelComponents.add(number);
				channelComponents.add(right);
			}
			
			channelComponents.add(new TextComponent(" "));
			
			for(BaseComponent c : channelComponents)
				c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/ch " + channel.getName()));
			
			components.addAll(channelComponents);
			
			first = false;
		}
		
		return ComponentSerializer.toString(components.toArray(new BaseComponent[components.size()]));
	}
}
