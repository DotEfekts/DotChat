package net.dotefekts.dotchat;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
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
	
	@PermissionHandler(node = "dotchat.join", description = "Allows user to join a channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "joinch", description = "Joins a channel.", format = "s<channel>", serverCommand = false)
	public boolean joinChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		ChatChannel channel = chatManager.getChannel(event.getArgs()[0]);
		if(channel == null || !chatManager.getPlayerManager(player).joinChannel(channel)) {
			player.sendMessage(ChatColor.RED + "That channel does not exist or you do not have permission to join it.");
		}
		
		return true;
	}
}
