package net.dotefekts.dotchat;

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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ChatReplayManager implements Listener {
	
	private ChatManager chatManager;
	
	public ChatReplayManager(DotChat plugin, ProtocolManager protocolManager, ChatManager chatManager) {
		this.chatManager = chatManager;
		
		protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				ChatType messageType = event.getPacket().getChatTypes().readSafely(0);
				if(messageType != ChatType.GAME_INFO) {
					event.setCancelled(true);
					PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
					String messageJson = event.getPacket().getChatComponents().readSafely(0).getJson();
					String messagePlain = BaseComponent.toPlainText(ComponentSerializer.parse(messageJson));
					
					if(messagePlain.startsWith(ChatChannel.MARKER_PREFIX)) {
						String channelName = messagePlain.substring(ChatChannel.MARKER_PREFIX.length(), messagePlain.indexOf(ChatChannel.MARKER_SUFFIX));
						ChatChannel sourceChannel = chatManager.getChannel(channelName);

						String fixedMessage = messageJson.replace(ChatChannel.MARKER_PREFIX + channelName + ChatChannel.MARKER_SUFFIX, "");
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
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void checkCanSend(AsyncPlayerChatEvent event) {
		PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
		ChatChannel currentChannel = manager.getActiveChatChannel();
		
		if(event.getMessage().contains(ChatChannel.MARKER_PREFIX)) {
			ChatChannel newChannel = tryGetChannelName(event.getMessage());
			if(newChannel != null) {
				if(manager.inChannel(newChannel))
					currentChannel = newChannel;
				else {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "You have not joined that channel.");
					return;
				}
			}
		}
		
		if(!currentChannel.canTalk()) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot send messages to this channel.");
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void chatEvent(AsyncPlayerChatEvent event) {
		PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
		ChatChannel currentChannel = manager.getActiveChatChannel();
		
		if(event.getMessage().contains(ChatChannel.MARKER_PREFIX)) {
			ChatChannel newChannel = tryGetChannelName(event.getMessage());
			if(newChannel != null) {
				currentChannel = newChannel;
			}
			
			event.setMessage(replaceMessageMarker(event.getMessage()));
		}
		
		String message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
		event.setFormat(ChatChannel.MARKER_PREFIX + currentChannel.getName() + ChatChannel.MARKER_SUFFIX + event.getFormat());
		chatManager.addMessageHistory(currentChannel, ChatUtilities.buildChatPacket(message, false, false));
	}

	private String replaceMessageMarker(String message) {
		String channelName = message.substring(message.indexOf(ChatChannel.MARKER_PREFIX) + ChatChannel.MARKER_PREFIX.length());
		channelName = channelName.substring(0, channelName.indexOf(ChatChannel.MARKER_SUFFIX));
		
		return message.replace(ChatChannel.MARKER_PREFIX + channelName + ChatChannel.MARKER_SUFFIX, "");
	}

	private ChatChannel tryGetChannelName(String channelName) {
		channelName = channelName.substring(channelName.indexOf(ChatChannel.MARKER_PREFIX) + ChatChannel.MARKER_PREFIX.length());
		channelName = channelName.substring(0, channelName.indexOf(ChatChannel.MARKER_SUFFIX));
		
		ChatChannel newChannel = chatManager.getChannel(channelName);
		
		return newChannel;
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
