package net.dotefekts.dotchat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class ChatChannel {
	public static final int CHAT_LIMIT = 100;
	public static final String MARKER_PREFIX = "\u0091[";
	public static final String MARKER_SUFFIX = "] ";
	public static final String PERMISSION_PREFIX = "dotchat.channel.";
	
	
	public static PacketContainer BLANK_MESSAGE;
	
	private String channelName;
	private int channelOrder;
	private String displayName;
	private String displayNameActive;
	private boolean isMultiChannel;
	private boolean isPublic;
	private boolean autoJoin;
	private boolean canLeave;
	private boolean canTalk;
	private boolean sendHistory;
	
	private Queue<Message> chatMessages;
	
	static {
		BLANK_MESSAGE = ChatUtilities.buildChatPacket("", false, true);
	}
	
	public ChatChannel(String channelName, int channelOrder, String displayName, String displayNameActive, boolean isMultiChannel, boolean isPublic, boolean autoJoin, boolean canLeave, boolean canTalk, boolean sendHistory) {
		this.channelName = channelName;
		this.channelOrder = channelOrder;
		this.displayName = displayName;
		this.displayNameActive = displayNameActive;
		this.isMultiChannel = isMultiChannel;
		this.isPublic = isPublic;
		this.autoJoin = autoJoin;
		this.canLeave = canLeave;
		this.canTalk = canTalk;
		this.sendHistory = sendHistory;
		
		chatMessages = new ConcurrentLinkedQueue<Message>();
		for(int i = 0; i < CHAT_LIMIT; i++)
			chatMessages.add(new Message(BLANK_MESSAGE.shallowClone(), 0));
	}
	
	public void addMessage(PacketContainer packet) {
		if(packet.getType() != PacketType.Play.Server.CHAT)
			return;
		
		chatMessages.add(new Message(packet, System.currentTimeMillis()));
		while(chatMessages.size() > CHAT_LIMIT) {
			chatMessages.poll();
		}
	}

	public String getName() {
		return channelName;
	}

	public int getOrder() {
		return channelOrder;
	}
	
	public String getDisplayName(boolean active) {
		return active ? displayNameActive : displayName;
	}
	
	public boolean canJoin(Player player) {
		return isPublic || player.hasPermission(PERMISSION_PREFIX + channelName);
	}
	
	public boolean isMultiChannel() {
		return isMultiChannel;
	}

	public boolean isPublic() {
		return isPublic;
	}
	
	public boolean isAutoJoin() {
		return autoJoin;
	}
	
	public boolean canLeave() {
		return canLeave;
	}
	
	public boolean canTalk() {
		return canTalk;
	}
	
	public boolean sendHistory() {
		return sendHistory;
	}
	
	public Queue<Message> getHistory() {
		if(sendHistory) {
			return new ConcurrentLinkedQueue<Message>(chatMessages);
		} else {
			Queue<Message> messages =  new ConcurrentLinkedQueue<Message>();
			for(int i = 0; i < CHAT_LIMIT; i++)
				messages.add(new Message(BLANK_MESSAGE.shallowClone(), 0));
			return messages;
		}
	}
}
