package net.dotefekts.dotchat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	private SortedMap<ChatChannel, Queue<Message>> channelMessages;
	private HashMap<ChatChannel, Integer> unreadMessages;
	
	public PlayerChatManager(ProtocolManager protocolManager, ChatManager chatManager, Format formatting, Player player, ChatChannel defaultChannel, ChatChannel defaultChatChannel, List<ChatChannel> channels) {
		this.protocolManager = protocolManager;
		this.chatManager = chatManager;
		this.formatting = formatting;
		this.player = player;
		this.activeChannel = defaultChannel;
		this.activeChatChannel = defaultChatChannel;
		this.lastAvailableChatChannel = defaultChatChannel;
		this.channelMessages = new TreeMap<ChatChannel, Queue<Message>>((a, b) -> (a != null && b != null ? a.getOrder() - b.getOrder() : (b != null ? 1 : (a != null ? -1 : 0))));
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
		
		rebuildAllMessages();
	}

	public List<String> getChannelList(boolean includeAll, boolean canLeaveOnly) {
		return getChannelList(includeAll, canLeaveOnly, "");
	}


	public List<String> getChannelList(boolean includeAll, boolean canLeaveOnly, String prefix) {
		List<String> channels = new ArrayList<String>();
		for(ChatChannel c : channelMessages.keySet())
			if((includeAll || !c.isMultiChannel()) && (!canLeaveOnly || c.canLeave()) && c.getName().startsWith(prefix))
				channels.add(c.getName());
		return channels;
	}
	
	
//	private void sendScoreboardPlayer(ChatChannel channel) {
//		PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
//		
//		WrappedGameProfile gameProfile = new WrappedGameProfile(UUID.nameUUIDFromBytes(channel.getName().getBytes()), "/tch " + channel.getName());
//		gameProfile.getProperties().put("textures", new WrappedSignedProperty("textures", SkinTextures.TAB_BLANK.Texture, SkinTextures.TAB_BLANK.Signature));
//		
//		PlayerInfoData playerInfo = new PlayerInfoData(gameProfile, 0, NativeGameMode.NOT_SET, WrappedChatComponent.fromText(""));
//		List<PlayerInfoData> infoDataList = new ArrayList<PlayerInfoData>();
//		infoDataList.add(playerInfo);
//		
//		packet.getPlayerInfoAction().write(0, PlayerInfoAction.ADD_PLAYER);
//		packet.getPlayerInfoDataLists().write(0, infoDataList);
//		
//		try {
//			protocolManager.sendServerPacket(player, packet);
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
//	}

	public void sendAllMessage(PacketContainer message) {
		long currTime = System.currentTimeMillis();
		for(Queue<Message> channelQueue : channelMessages.values()) {
			channelQueue.add(new Message(message, currTime));
			while(channelQueue.size() > ChatChannel.CHAT_LIMIT) {
				channelQueue.poll();
			}
		}

		sendMessages();
	}
	
	public void sendMessage(ChatChannel sourceChannel, PacketContainer message) {
		if(channelMessages.containsKey(sourceChannel)) {
			long currTime = System.currentTimeMillis();
			Queue<Message> channelQueue = channelMessages.get(sourceChannel);
			
			channelQueue.add(new Message(message, currTime));
			while(channelQueue.size() > ChatChannel.CHAT_LIMIT) {
				channelQueue.poll();
			}

			if(chatManager.getMultiChannel() != null) {
				PacketContainer prefixedMessage = ChatUtilities.addChannelPrefix(message, formatting.getAllSourceName(sourceChannel));
				
				Queue<Message> multiChannelQueue = channelMessages.get(chatManager.getMultiChannel());
				multiChannelQueue.add(new Message(prefixedMessage, currTime));
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
			
			rebuildAllMessages();
			
			switchChannel(channel);
			
			return true;
		}
		
		return false;
	}
	
	public boolean inChannel(ChatChannel channel) {
		return channelMessages.containsKey(channel);
	}

	public boolean leaveChannel(ChatChannel channel) {
		if(channelMessages.containsKey(channel)) {
			if(!channel.canLeave())
				return false;
			
			if(activeChannel == channel)
				activeChannel = chatManager.getDefaultChannel();
			
			if(activeChatChannel == channel) {
				activeChatChannel = chatManager.getDefaultChatChannel();
				if(activeChatChannel.canTalk())
					lastAvailableChatChannel = chatManager.getDefaultChatChannel();
			}
			
			if(lastAvailableChatChannel == channel)
				lastAvailableChatChannel = chatManager.getDefaultChatChannel();
			
			channelMessages.remove(channel);
			unreadMessages.remove(channel);
			
			rebuildAllMessages();
			sendMessages();
		}
		
		return true;
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

	public boolean switchChatChannel(ChatChannel channel) {
		if(channelMessages.containsKey(channel)) {
			activeChatChannel = channel;
			
			if(!activeChannel.isMultiChannel())
				activeChannel = channel;
			
			if(channel.canTalk())
				lastAvailableChatChannel = channel;
			
			sendMessages();
			
			return true;
		} else {
			return false;
		}
	}

	private void rebuildAllMessages() {
		if(chatManager.getMultiChannel() != null && channelMessages.get(chatManager.getMultiChannel()) != null) {
			channelMessages.get(chatManager.getMultiChannel()).clear();
			List<Message> messages = new ArrayList<Message>();

			for(int i = 0; i < ChatChannel.CHAT_LIMIT; i++)
				messages.add(new Message(ChatChannel.BLANK_MESSAGE.shallowClone(), 0));
			
			for(Entry<ChatChannel, Queue<Message>> channelEntry : channelMessages.entrySet())
				for(Message message : channelEntry.getValue())
					if(message.getTime() != 0)
						messages.add(new Message(
								ChatUtilities.addChannelPrefix(
									message.getPacket(), 
									formatting.getAllSourceName(channelEntry.getKey())
								),
								message.getTime()));
			
			messages.sort((a, b) -> a.getTime() < b.getTime() ? -1 : a.getTime() > b.getTime() ? 1 : 0);
			
			Queue<Message> newQueue = new ConcurrentLinkedQueue<Message>(messages);
			while(newQueue.size() > ChatChannel.CHAT_LIMIT) {
				newQueue.poll();
			}
			
			channelMessages.put(chatManager.getMultiChannel(), newQueue);
		}
	}
	
	private void sendMessages() {
		for(Message message : channelMessages.get(activeChannel)) {
			try {
				protocolManager.sendServerPacket(player, message.getPacket(), false);
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

	public List<String> getAvailableChannels() {
		return getAvailableChannels("");
	}

	public List<String> getAvailableChannels(String prefix) {
		List<String> playerAvailableChannels = new ArrayList<String>();
		List<ChatChannel> availableChannels = chatManager.getChannels();
		
		for(ChatChannel channel : availableChannels)
			if(!channelMessages.containsKey(channel) && channel.canJoin(player) && channel.getName().startsWith(prefix))
				playerAvailableChannels.add(channel.getName());
		
		return playerAvailableChannels;
	}
}
