package net.dotefekts.dotchat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

public class ChatChannel {
	public static final int CHAT_LIMIT = 100;
	private static final String PERMISSION_PREFIX = "dotchat.channel.";
	
	public static PacketContainer BLANK_MESSAGE;
	
	private String channelName;
	private int channelOrder;
	private String displayName;
	private boolean isMultiChannel;
	private boolean isPublic;
	private boolean autoJoin;
	private boolean canLeave;
	private boolean canTalk;
	private boolean sendHistory;
	
	private Queue<PacketContainer> chatMessages;
	
	static {
		BLANK_MESSAGE = ChatUtilities.buildChatPacket("", false, true);
	}
	
	public ChatChannel(String channelName, int channelOrder, String displayName, boolean isMultiChannel, boolean isPublic, boolean autoJoin, boolean canLeave, boolean canTalk, boolean sendHistory) {
		this.channelName = channelName;
		this.channelOrder = channelOrder;
		this.displayName = displayName;
		this.isMultiChannel = isMultiChannel;
		this.isPublic = isPublic;
		this.autoJoin = autoJoin;
		this.canLeave = canLeave;
		this.canTalk = canTalk;
		this.sendHistory = sendHistory;
		
		chatMessages = new ConcurrentLinkedQueue<PacketContainer>();
		for(int i = 0; i < CHAT_LIMIT; i++)
			chatMessages.add(BLANK_MESSAGE.shallowClone());
	}
	
	public void addMessage(PacketContainer packet) {
		if(packet.getType() != PacketType.Play.Server.CHAT)
			return;
		
		chatMessages.add(packet);
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
	
	public String getDisplayName() {
		return displayName;
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
	
	public Queue<PacketContainer> getHistory() {
		if(sendHistory) {
			return new ConcurrentLinkedQueue<PacketContainer>(chatMessages);
		} else {
			Queue<PacketContainer> messages =  new ConcurrentLinkedQueue<PacketContainer>();
			for(int i = 0; i < CHAT_LIMIT; i++)
				messages.add(BLANK_MESSAGE.shallowClone());
			return messages;
		}
	}
}
