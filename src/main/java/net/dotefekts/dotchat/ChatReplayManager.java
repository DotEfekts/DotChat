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
	private static final String MARKER_PREFIX = "\u0091[";
	private static final String MARKER_SUFFIX = "] ";
	
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
					
					if(messagePlain.startsWith(MARKER_PREFIX)) {
						String channelName = messagePlain.substring(MARKER_PREFIX.length(), messagePlain.indexOf(MARKER_SUFFIX));
						ChatChannel sourceChannel = chatManager.getChannel(channelName);

						String fixedMessage = messageJson.replace(MARKER_PREFIX + channelName + MARKER_SUFFIX, "");
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
		if(!manager.getActiveChatChannel().canTalk()) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot send messages to this channel.");
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void chatEvent(AsyncPlayerChatEvent event) {
		PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
		String message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
		
		event.setFormat(MARKER_PREFIX + manager.getActiveChatChannel().getName() + MARKER_SUFFIX + event.getFormat());

		chatManager.addMessageHistory(manager.getActiveChatChannel(), ChatUtilities.buildChatPacket(message, false, false));
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
