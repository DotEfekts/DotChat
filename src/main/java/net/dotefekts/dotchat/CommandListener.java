package net.dotefekts.dotchat;

import java.util.Arrays;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.permissions.PermissionDefault;

import net.dotefekts.dotutils.commandhelper.CommandEvent;
import net.dotefekts.dotutils.commandhelper.CommandHandler;
import net.dotefekts.dotutils.commandhelper.PermissionHandler;
import net.md_5.bungee.api.ChatColor;

public class CommandListener implements Listener {
	private ChatManager chatManager;
	
	public CommandListener(ChatManager chatManager) {
		this.chatManager = chatManager;
	}
	
	@PermissionHandler(node = "dotchat.switch", description = "Allows user to switch channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "ch", description = "Switches current channel.", format = "s<channel>", serverCommand = false)
	public boolean switchChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		ChatChannel channel = chatManager.getChannel(event.getArgs()[0]);
		if(!chatManager.getPlayerManager(player).switchChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		}
		return true;
	}

	@PermissionHandler(node = "dotchat.switch", description = "Allows user to switch channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "tch", description = "Switches current chat channel when talking in all.", format = "s<channel>", serverCommand = false)
	public boolean switchChatChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		ChatChannel channel = chatManager.getChannel(event.getArgs()[0]);
		if(!chatManager.getPlayerManager(player).switchChatChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		}
		return true;
	}
	
	@PermissionHandler(node = "dotchat.switch", description = "Allows user to switch channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "t", description = "Sends a message to the specified channel without switching.", format = "s<channel> ...", serverCommand = false)
	public boolean sendMessage(CommandEvent event) {
		Player player = (Player) event.getSender();
		ChatChannel channel = chatManager.getChannel(event.getArgs()[0]);
		
		if(!chatManager.getPlayerManager(player).inChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		} else if(event.getArgs().length <= 1) {
			player.sendMessage(ChatColor.RED + "You didn't type anything.");
		} else if(event.getArgs().length > 1) {
			String message = String.join(" ", Arrays.copyOfRange(event.getArgs(), 1, event.getArgs().length));
			player.chat(ChatChannel.MARKER_PREFIX + channel.getName() + ChatChannel.MARKER_SUFFIX + message);
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.join", description = "Allows user to join a channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "join", description = "Joins a channel.", format = "s<channel>", serverCommand = false)
	public boolean joinChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		ChatChannel channel = chatManager.getChannel(event.getArgs()[0]);
		
		if(channel == null || !chatManager.getPlayerManager(player).joinChannel(channel)) {
			player.sendMessage(ChatColor.RED + "That channel does not exist or you do not have permission to join it.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.leave", description = "Allows a user to leave a channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "leave", description = "Joins a channel.", format = "s<channel>", serverCommand = false)
	public boolean leaveChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		ChatChannel channel = chatManager.getChannel(event.getArgs()[0]);
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		if(!manager.inChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		} else if(!manager.leaveChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You cannot leave that channel.");
		}
		
		return true;
	}

	
	@EventHandler
	public void tabCompleteSwitch(TabCompleteEvent event) {
		if(event.getBuffer().startsWith("/t") || event.getBuffer().startsWith("/tch")|| event.getBuffer().startsWith("/ch")) {
			String[] split = event.getBuffer().split(" ");
			if(split.length < 3) {
				Player player = (Player) event.getSender();
				PlayerChatManager playerManager = chatManager.getPlayerManager(player);
				
				if(split.length == 2) {
					event.setCompletions(playerManager.getChannelList(event.getBuffer().startsWith("/ch"), false, split[1]));
				} else {
					event.setCompletions(playerManager.getChannelList(event.getBuffer().startsWith("/ch"), false));
				}
			}
		} else if(event.getBuffer().startsWith("/join")) {
			String[] split = event.getBuffer().split(" ");
			
			if(split.length < 3) {
				Player player = (Player) event.getSender();
				PlayerChatManager playerManager = chatManager.getPlayerManager(player);
				
				if(split.length == 2) {
					event.setCompletions(playerManager.getAvailableChannels(split[1]));
				} else {
					event.setCompletions(playerManager.getAvailableChannels());
				}
			}
		} else if(event.getBuffer().startsWith("/leave")) {
			String[] split = event.getBuffer().split(" ");
			
			if(split.length < 3) {
				Player player = (Player) event.getSender();
				PlayerChatManager playerManager = chatManager.getPlayerManager(player);
				
				if(split.length == 2) {
					event.setCompletions(playerManager.getChannelList(false, true, split[1]));
				} else {
					event.setCompletions(playerManager.getChannelList(false, true));
				}
			}
		}
	}
}
