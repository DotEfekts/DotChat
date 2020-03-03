package net.dotefekts.dotchat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import net.dotefekts.dotchat.Format.ChatType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class PlayerChatManager {
	private ProtocolManager protocolManager;
	private ChatManager chatManager;
	private Format formatter;
	private Player player;
	private ChatChannel activeChannel;
	private ChatChannel lastAvailableChatChannel;
	private ChatChannel activeChatChannel;
	private PartyChannel partyChannel = null;
	private boolean partyInviteDisabled = false;
	private boolean pmsDisabled = false;
	private int pmOrder;
	private Map<String, PmChannel> pmChannels;
	private SortedMap<ChatChannel, Queue<Message>> channelMessages;
	private Map<ChatChannel, Integer> unreadMessages;
	
	public PlayerChatManager(ProtocolManager protocolManager, ChatManager chatManager, int pmOrder, Format formatting, Player player, ChatChannel defaultChannel, ChatChannel defaultChatChannel, List<ChatChannel> channels) {
		this.protocolManager = protocolManager;
		this.chatManager = chatManager;
		this.formatter = formatting;
		this.player = player;
		this.activeChannel = defaultChannel;
		this.activeChatChannel = defaultChatChannel;
		this.lastAvailableChatChannel = defaultChatChannel;
		this.pmOrder = pmOrder;
		this.pmChannels = new HashMap<String, PmChannel>();
		this.unreadMessages = new HashMap<ChatChannel, Integer>();
		
		this.channelMessages = new TreeMap<ChatChannel, Queue<Message>>((a, b) -> (a != null && b != null ? a.getOrder() - b.getOrder() : (b != null ? 1 : (a != null ? -1 : 0))));
		
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
	
	public PartyChannel createPartyChannel() {
		if(partyChannel == null) {
			if(chatManager.getPartyOrder() != -1) {
				PartyChannel newPartyChannel = new PartyChannel(
						chatManager.getPartyOrder(), 
						formatter.getPartyTabName(), 
						formatter.getPartyTabNameActive(), 
						true, 
						chatManager.sendPartyHistory());
				
				newPartyChannel.addInvitedPlayer(player.getUniqueId());
				joinPartyChannel(newPartyChannel, false);
			} else {
				return null;
			}
		}
		
		return partyChannel;
	}
	
	public PartyChannel getPartyChannel() {
		return partyChannel;
	}

	public void disablePartyInvite() {
		partyInviteDisabled = true;
	}

	public void enablePartyInvite() {
		partyInviteDisabled = false;
	}

	public boolean partyInviteEnabled() {
		return !partyInviteDisabled;
	}

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
				PacketContainer prefixedMessage = ChatUtilities.addChannelPrefix(message, formatter.getAllSourceName(sourceChannel));
				
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

	public PmChannel openPmChannel(Player partnerPlayer, boolean silent) {		
		PmChannel channel = null;
		if(pmChannels.containsKey(partnerPlayer.getName()))
			channel = pmChannels.get(partnerPlayer.getName());

		PlayerChatManager partnerManager = chatManager.getPlayerManager(partnerPlayer);
		if((channel == null || !channelMessages.containsKey(channel)) && !partnerManager.pmsEnabled())
			return null;
		
		if(channel == null) {
			channel = new PmChannel(
					player, 
					partnerPlayer, 
					pmOrder++, 
					formatter.getPmTabName(partnerPlayer),
					formatter.getPmTabNameActive(partnerPlayer),
					chatManager.sendPmHistory());
			pmChannels.put(partnerPlayer.getName(), channel);
			partnerManager.openPmChannel(player, true);
		}
		
		joinChannel(channel, silent);
		
		return channel;
	}

	public ChatChannel getPmChanel(String senderName, String recieverName) {
		if(senderName == player.getName()) {
			return pmChannels.get(recieverName);
		} else if(recieverName == player.getName()) {
			return pmChannels.get(senderName);
		} else {
			return null;
		}
	}
	
	public void disablePms() {
		pmsDisabled = true;
	}

	public void enablePms() {
		pmsDisabled = false;
	}

	public boolean pmsEnabled() {
		return !pmsDisabled;
	}
	
	public ChatChannel getActiveChannel() {
		return activeChannel;
	}
	
	public ChatChannel getActiveChatChannel() {
		return activeChatChannel;
	}
	
	public boolean joinChannel(ChatChannel channel, boolean silent) {
		if(channel.canJoin(player)) {
			channelMessages.put(channel, channel.getHistory());
			unreadMessages.put(channel, 0);
			
			rebuildAllMessages();
			
			if(!silent)
				switchChannel(channel);
			else
				sendMessages();
			
			return true;
		}
		
		return false;
	}
	
	public boolean joinPartyChannel(PartyChannel channel, boolean silent) {
		if(partyChannel != null)
			return false;
		
		boolean joinResult = joinChannel(channel, silent);
		if(joinResult) {
			partyChannel = channel;
			partyChannel.addPlayer(player);
		}
		
		return joinResult;
	}
	
	public boolean inChannel(ChatChannel channel) {
		return channelMessages.containsKey(channel);
	}

	public boolean leaveChannel(ChatChannel channel) {
		if(channelMessages.containsKey(channel)) {
			if(channel.canJoin(player) && !channel.canLeave())
				return false;
			
			if(partyChannel == channel) {
				partyChannel.removePlayer(player);
				partyChannel = null;
			}
			
			if(activeChannel == channel)
				switchChannel(chatManager.getDefaultChannel());
			
			if(activeChatChannel == channel)
				switchChatChannel(chatManager.getDefaultChatChannel());
			
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
									formatter.getAllSourceName(channelEntry.getKey())
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
		Set<ChatChannel> joinedChannels = channelMessages.keySet();
		
		List<ChatChannel> toLeave = new ArrayList<ChatChannel>();
		for(ChatChannel channel : joinedChannels)
			if(!channel.canJoin(player))
				toLeave.add(channel);
		for(ChatChannel channel : toLeave)
			leaveChannel(channel);
			
		for(ChatChannel channel : chatManager.getChannels())
			if(channel.isAutoJoin() && !joinedChannels.contains(channel) && channel.canJoin(player))
				joinChannel(channel, true);
		
		for(Message message : channelMessages.get(activeChannel)) {
			try {
				protocolManager.sendServerPacket(player, message.getPacket(), false);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		try {
			if(formatter.getChatSeparator() != null)
				protocolManager.sendServerPacket(player, ChatUtilities.buildChatPacket(ComponentSerializer.toString(formatter.getChatSeparator()), true, true), false);
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
				components.addAll(Arrays.asList(formatter.getTabSeparator()));
			}
			
			Format.ChatType type;
			if(activeChannel == channel) {
				type = ChatType.ACTIVE;
			} else if(activeChatChannel == channel) {
				type = ChatType.CHATTING;
			} else {
				type = ChatType.INACTIVE;
			}
			
			List<BaseComponent> channelComponents = new ArrayList<BaseComponent>(Arrays.asList(formatter.getTabName(channel, type, activeChannel.isMultiChannel(), unreadMessages.get(channel))));
			
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

	public void destroy() {
		if(partyChannel != null)
			partyChannel.removePlayer(player);
		channelMessages.clear();
		unreadMessages.clear();
	}
}
