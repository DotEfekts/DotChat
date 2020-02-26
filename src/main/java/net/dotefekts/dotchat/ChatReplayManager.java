package net.dotefekts.dotchat;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ChatReplayManager implements Listener {
	private static final String CHAT_PREFIX = "dotchat.p:";
	
	private ProtocolManager protocolManager;
	private DotChat plugin;
	private ChatManager chatManager;
	private HashMap<String, ChatChannel> messageChannels;
	
	public ChatReplayManager(DotChat plugin, ProtocolManager protocolManager, ChatManager chatManager) {
		this.plugin = plugin;
		this.protocolManager = protocolManager;
		this.chatManager = chatManager;
		this.messageChannels = new HashMap<String, ChatChannel>();
		
		protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				ChatType messageType = event.getPacket().getChatTypes().readSafely(0);
				if(messageType != ChatType.GAME_INFO) {
					event.setCancelled(true);
					PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
					String messageJson = event.getPacket().getChatComponents().readSafely(0).getJson();
					String messagePlain = BaseComponent.toPlainText(ComponentSerializer.parse(messageJson));
					
					if(messagePlain.startsWith(CHAT_PREFIX)) {
						UUID playerUUID = UUID.fromString(messagePlain.substring(CHAT_PREFIX.length(), messagePlain.indexOf(';')));
						ChatChannel sourceChannel = chatManager.getPlayerManager(playerUUID).getActiveChatChannel();

						String fixedMessage = messageJson.replace(CHAT_PREFIX + playerUUID.toString() + ";", "");
						WrappedChatComponent chatComponent = event.getPacket().getChatComponents().readSafely(0);
						chatComponent.setJson(fixedMessage);
						event.getPacket().getChatComponents().write(0, chatComponent);
						event.getPacket().getChatTypes().write(0, ChatType.CHAT);
						
						manager.sendMessage(sourceChannel, event.getPacket());
					} else {
						ChatChannel systemMessages = chatManager.getSystemMessageChannel();
						if(systemMessages != null)
							manager.sendMessage(systemMessages, event.getPacket());
						else
							manager.sendAllMessage(event.getPacket());
					}
				} 
			}
		});
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void chatEvent(AsyncPlayerChatEvent event) {
		PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
		String message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
		
		event.setFormat(CHAT_PREFIX + event.getPlayer().getUniqueId() + ";" + event.getFormat());
		
		messageChannels.put(message, manager.getActiveChatChannel());
		manager.getActiveChatChannel().addMessage(ChatUtilities.buildChatPacket(message, true, false));
	}

	@EventHandler
	public void playerJoin(PlayerJoinEvent event) {
		chatManager.getPlayerManager(event.getPlayer());
	}
	
	@EventHandler
	public void playerLeave(PlayerQuitEvent event) {
		chatManager.destroyPlayerManager(event.getPlayer());
	}
}
