package net.dotefekts.dotchat;

import javax.print.attribute.UnmodifiableSetException;

import org.bukkit.Bukkit;
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
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ChatReplayManager implements Listener {

	private DotChat plugin;
	private ChatManager chatManager;
	
	public ChatReplayManager(DotChat plugin, ProtocolManager protocolManager, ChatManager chatManager) {
		this.plugin = plugin;
		this.chatManager = chatManager;
		
		protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				ChatType messageType = packet.getChatTypes().readSafely(0);
				if(messageType != ChatType.GAME_INFO) {
					event.setCancelled(true);
					PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
					WrappedChatComponent chatComponent = packet.getChatComponents().readSafely(0);
					String messageJson = chatComponent != null ? chatComponent.getJson() : null;
					if(messageJson == null) {
						// Fallback to reflection to try and get/fix the message.
						Object packetRaw = packet.getHandle();
						try {
							BaseComponent[] components = (BaseComponent[]) packetRaw.getClass().getField("components").get(packetRaw);
							messageJson = ComponentSerializer.toString(components);
							// Rebuilt the chat packet to be used with ProtocolLib.
							packet = ChatUtilities.buildChatPacket(messageJson, true, messageType == ChatType.SYSTEM);
						} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
								| SecurityException e) {
							e.printStackTrace();
							plugin.getLogger().warning("Failed to get chat info. Discarding message.");
							event.getPlayer().sendMessage(ChatColor.RED + "An error occurred while attempting to recieve a message.");
							event.setCancelled(true);
							return;
						}
					}
					String messagePlain = BaseComponent.toPlainText(ComponentSerializer.parse(messageJson));
					
					if(messagePlain.startsWith(ChatChannel.MARKER_PREFIX)) {
						String channelName = messagePlain.substring(ChatChannel.MARKER_PREFIX.length(), messagePlain.indexOf(ChatChannel.MARKER_SUFFIX));
						
						ChatChannel sourceChannel;
						if(channelName.equalsIgnoreCase("party")) {
							sourceChannel = manager.getPartyChannel();
						} else if(channelName.startsWith("p:")) {
							String senderName = channelName.split(";")[0].substring(2);
							String recieverName = channelName.split(";")[1];
							
							sourceChannel = manager.getPmChanel(senderName, recieverName);
						} else {
							sourceChannel = chatManager.getChannel(channelName);
						}

						String fixedMessage = messageJson.replace(ChatChannel.MARKER_PREFIX + channelName + ChatChannel.MARKER_SUFFIX, "");
						chatComponent.setJson(fixedMessage);
						packet.getChatComponents().write(0, chatComponent);
						packet.getChatTypes().write(0, ChatType.CHAT);
						
						manager.sendMessage(sourceChannel, packet);
					} else {
						ChatChannel systemMessages = chatManager.getSystemMessageChannel();
						if(systemMessages != null)
							manager.sendMessage(systemMessages, packet);
						else
							manager.sendAllMessage(packet);
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
			String channelName = parseChannelName(event.getMessage());
			if(channelName.startsWith("p:")) {
				if(manager.pmsEnabled()) {
					PmChannel pmChannel = manager.openPmChannel(Bukkit.getPlayer(channelName.split(";")[1]), false);
					if(pmChannel != null && chatManager.getPlayerManager(pmChannel.getPartner()).pmsEnabled()) {
						currentChannel = pmChannel;
					} else {
						event.setCancelled(true);
						event.getPlayer().sendMessage(ChatColor.RED + "You cannot message that player.");
					}
				} else {
					event.setCancelled(true);
					event.getPlayer().sendMessage(ChatColor.RED + "You currently have PMs disabled.");
				}
			} else {
				ChatChannel newChannel = tryGetChannel(event.getMessage());
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
		}
		
		if(!currentChannel.canTalk()) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(ChatColor.RED + "You cannot send messages to this channel.");
		} else if(currentChannel instanceof PartyChannel) {
			try {
				event.getRecipients().clear();
				event.getRecipients().addAll(((PartyChannel) currentChannel).getPlayers());
			} catch (UnmodifiableSetException e) {
				event.setCancelled(true);
				e.printStackTrace();
				plugin.getLogger().severe("Discarded party chat message as recipient set was not modifiable.");
			}
		} else if(currentChannel instanceof PmChannel) {
			if(chatManager.getPlayerManager(((PmChannel) currentChannel).getPartner()).pmsEnabled()) {
				try {
					event.getRecipients().clear();
					event.getRecipients().add(((PmChannel) currentChannel).getOwner());
					event.getRecipients().add(((PmChannel) currentChannel).getPartner());
				} catch (UnmodifiableSetException e) {
					event.setCancelled(true);
					e.printStackTrace();
					plugin.getLogger().severe("Discarded private chat message as recipient set was not modifiable.");
				}
			} else {
				event.setCancelled(true);
				event.getPlayer().sendMessage(ChatColor.RED + "You cannot message that player.");
			}
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void chatEvent(AsyncPlayerChatEvent event) {
		PlayerChatManager manager = chatManager.getPlayerManager(event.getPlayer());
		ChatChannel currentChannel = manager.getActiveChatChannel();
		
		if(event.getMessage().contains(ChatChannel.MARKER_PREFIX)) {
			ChatChannel newChannel = tryGetChannel(event.getMessage());
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

	private ChatChannel tryGetChannel(String channelName) {
		return chatManager.getChannel(parseChannelName(channelName));
	}

	private String parseChannelName(String channelName) {
		channelName = channelName.substring(channelName.indexOf(ChatChannel.MARKER_PREFIX) + ChatChannel.MARKER_PREFIX.length());
		channelName = channelName.substring(0, channelName.indexOf(ChatChannel.MARKER_SUFFIX));		
		return channelName;
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
