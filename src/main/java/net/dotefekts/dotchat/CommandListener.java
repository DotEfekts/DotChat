package net.dotefekts.dotchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.permissions.PermissionDefault;

import net.dotefekts.dotutils.commandhelper.CommandEvent;
import net.dotefekts.dotutils.commandhelper.CommandHandler;
import net.dotefekts.dotutils.commandhelper.CommandHandlers;
import net.dotefekts.dotutils.commandhelper.PermissionHandler;
import net.md_5.bungee.api.ChatColor;

public class CommandListener implements Listener {
	private static final String PARTY_MESSAGE_PREFIX = ChatChannel.MARKER_PREFIX + "party" + ChatChannel.MARKER_SUFFIX;
	
	private ChatManager chatManager;
	
	public CommandListener(ChatManager chatManager) {
		this.chatManager = chatManager;
	}
	
	@PermissionHandler(node = "dotchat.switch", description = "Allows user to switch channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "ch", description = "Switches current channel.", format = "s<channel>", serverCommand = false)
	public boolean switchChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		ChatChannel channel;
		if(event.getArgs()[0].equalsIgnoreCase("party"))
			channel = manager.getPartyChannel();
		else
			channel = chatManager.getChannel(event.getArgs()[0]);
		
		if(channel == null || !chatManager.getPlayerManager(player).switchChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		}
		return true;
	}

	@PermissionHandler(node = "dotchat.switch", description = "Allows user to switch channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "tch", description = "Switches current chat channel when talking in all.", format = "s<channel>", serverCommand = false)
	public boolean switchChatChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		ChatChannel channel;
		if(event.getArgs()[0].equalsIgnoreCase("party"))
			channel = manager.getPartyChannel();
		else
			channel = chatManager.getChannel(event.getArgs()[0]);
		
		if(channel == null || !chatManager.getPlayerManager(player).switchChatChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		}
		return true;
	}
	
	@PermissionHandler(node = "dotchat.switch", description = "Allows user to switch channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "t", description = "Sends a message to the specified channel without switching.", format = "s<channel> ...", serverCommand = false)
	public boolean sendMessage(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		ChatChannel channel;
		if(event.getArgs()[0].equalsIgnoreCase("party"))
			channel = manager.getPartyChannel();
		else
			channel = chatManager.getChannel(event.getArgs()[0]);
		
		
		if(channel == null || !chatManager.getPlayerManager(player).inChannel(channel)) {
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
		
		if(channel == null || !chatManager.getPlayerManager(player).joinChannel(channel, false)) {
			player.sendMessage(ChatColor.RED + "That channel does not exist or you do not have permission to join it.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.leave", description = "Allows a user to leave a channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandlers(value = { 
			@CommandHandler(command = "leave", description = "Leaves a channel.", format = "s[channel]", serverCommand = false),
			@CommandHandler(command = "close", description = "Leaves a channel.", format = "s[channel]", serverCommand = false) 
		})
	public boolean leaveChannel(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		ChatChannel channel;
		
		if(event.getArgs().length == 1) {
			if(event.getArgs()[0].equalsIgnoreCase("party")) {
				channel = manager.getPartyChannel();
			} else if(event.getArgs()[0].startsWith("p:")) {
				String senderName = event.getArgs()[0].split(";")[0].substring(2);
				String recieverName = event.getArgs()[0].split(";")[1];
				
				channel = manager.getPmChanel(senderName, recieverName);
			} else {
				channel = chatManager.getChannel(event.getArgs()[0]);
			}
			
			if(channel == null) {
				channel = manager.getPmChanel(player.getName(), event.getArgs()[0]);
			}
		} else {
			channel = manager.getActiveChannel();
		}
		 
		
		if(!manager.inChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You have not joined that channel.");
		} else if(!manager.leaveChannel(channel)) {
			player.sendMessage(ChatColor.RED + "You cannot leave " + (event.getArgs().length == 1 ? "that" : "this") + " channel.");
		} else if(channel.getName() == "party") {
			for(Player member : ((PartyChannel) channel).getPlayers())
				member.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + player.getDisplayName() + " left the party.");
			player.sendMessage(ChatColor.YELLOW + "You have left the party.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.private", description = "Allows user to send a private message.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "msg", description = "Sends a private message to a player.", format = "p<User> ...", serverCommand = false)
	@CommandHandler(command = "pm", description = "Sends a private message to a player.", format = "p<User> ...", serverCommand = false)
	@CommandHandler(command = "m", description = "Sends a private message to a player.", format = "p<User> ...", serverCommand = false)
	public boolean privateMessage(CommandEvent event) {
		Player player = (Player) event.getSender();
		
		if(event.getArgs().length > 1) {
			Player recievingPlayer = Bukkit.getPlayer(event.getArgs()[0]);
			if(player != recievingPlayer && chatManager.getPlayerManager(recievingPlayer).pmsEnabled()) {
				String message = String.join(" ", Arrays.copyOfRange(event.getArgs(), 1, event.getArgs().length));
				player.chat(ChatChannel.MARKER_PREFIX + "p:" + player.getName() + ";" + recievingPlayer.getName() + ChatChannel.MARKER_SUFFIX + message);
			} else {
				player.sendMessage(ChatColor.RED + "You cannot message that player.");
			}
		} else {
			player.sendMessage(ChatColor.RED + "You didn't type anything.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.private.disable", description = "Allows user to disable and enable sending and receiving of private messages.", permissionDefault = PermissionDefault.OP)
	@CommandHandler(command = "pmoff", description = "Disables sending and recieving of private messages.", format = "n", serverCommand = false)
	public boolean pmOff(CommandEvent event) {
		Player player = (Player) event.getSender();
		chatManager.getPlayerManager(player).disablePms();
		player.sendMessage(ChatColor.YELLOW + "Private messages have been disabled.");
		return true;
	}
	
	@PermissionHandler(node = "dotchat.private.disable", description = "Allows user to disable and enable sending and receiving of private messages.", permissionDefault = PermissionDefault.OP)
	@CommandHandler(command = "pmon", description = "Enables sending and recieving of private messages.", format = "n", serverCommand = false)
	public boolean pmOn(CommandEvent event) {
		Player player = (Player) event.getSender();
		chatManager.getPlayerManager(player).enablePms();
		player.sendMessage(ChatColor.YELLOW + "Private messages have been enabled.");
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.create", description = "Allows a user to create a party.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party create", description = "Creates a party and optionally invites users.", format = "...", serverCommand = false)
	public boolean createParty(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		if(manager.getPartyChannel() == null) {
			PartyChannel channel = manager.createPartyChannel();
			
			if(event.getArgs().length > 0) {
				for(String name : event.getArgs()) {
					Player invPlayer = Bukkit.getPlayer(name);
					if(invPlayer != null && invPlayer != player && chatManager.getPlayerManager(invPlayer).partyInviteEnabled()) {
						channel.addInvitedPlayer(invPlayer.getUniqueId());
						invPlayer.sendMessage(ChatColor.YELLOW + "You've been invited to " + player.getDisplayName() + "'s chat party. Type " + ChatColor.GOLD + "/party join " + player.getDisplayName() + ChatColor.YELLOW + " to accept.");
					}
				}
				
				player.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + "Party created and players invited.");
			} else {
				player.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + "Party has been created.");
			}
		} else {
			player.sendMessage(ChatColor.RED + "You are already in a party.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.invite", description = "Allows a user to invite users to an existing party.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party invite", description = "Invites users to a party.", format = "...", serverCommand = false)
	public boolean partyInvite(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		PartyChannel channel = manager.getPartyChannel();
		if(channel != null) {
			if(event.getArgs().length > 0) {
				for(String name : event.getArgs()) {
					Player invPlayer = Bukkit.getPlayer(name);
					if(invPlayer != null && invPlayer != player && chatManager.getPlayerManager(invPlayer).partyInviteEnabled()) {
						channel.addInvitedPlayer(invPlayer.getUniqueId());
						invPlayer.sendMessage(ChatColor.YELLOW + "You've been invited to " + player.getDisplayName() + "'s chat party. Type " + ChatColor.GOLD + "/party join " + player.getDisplayName() + ChatColor.YELLOW + " to accept.");
					}
				}
				
				player.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + "Players invited to party.");
			} else {
				player.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.RED + "You must specify players to invite.");
			}
		} else {
			player.sendMessage(ChatColor.RED + "You are not in a party.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.disable", description = "Allows user to disable and enable receiving of party invites.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party inviteoff", description = "Disables recieving party invites.", format = "n", serverCommand = false)
	public boolean partyInviteOff(CommandEvent event) {
		Player player = (Player) event.getSender();
		chatManager.getPlayerManager(player).disablePartyInvite();
		player.sendMessage(ChatColor.YELLOW + "Party invites have been disabled.");
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.disable", description = "Allows user to disable and enable receiving of party invites.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party inviteon", description = "Enables recieving party invites.", format = "n", serverCommand = false)
	public boolean partyInviteOn(CommandEvent event) {
		Player player = (Player) event.getSender();
		chatManager.getPlayerManager(player).enablePartyInvite();
		player.sendMessage(ChatColor.YELLOW + "Party invites have been enabled.");
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.join", description = "Allows a user to join an existing party.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party join", description = "Accepts a users invite to a party.", format = "p<User>", serverCommand = false)
	public boolean partyJoin(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		if(manager.getPartyChannel() == null) {
			Player partyPlayer = Bukkit.getPlayer(event.getArgs()[0]);
			
			if(partyPlayer != null) {
				PartyChannel partyChannel = chatManager.getPlayerManager(partyPlayer).getPartyChannel();
				if(partyChannel == null || !manager.joinPartyChannel(partyChannel, false)) {
					player.sendMessage(ChatColor.RED + "You have not been invited to that players party.");
				} else {
					for(Player member : manager.getPartyChannel().getPlayers())
						if(member != player)
							member.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + player.getDisplayName() + " joined the party.");
					player.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + "You have joined " + partyPlayer.getDisplayName() + "'s party.");
				}
			} else {
				player.sendMessage(ChatColor.RED + "Player is not online.");
			}
		} else {
			player.sendMessage(ChatColor.RED + "You are already in a party.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.leave", description = "Allows a user to leave a channel.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party leave", description = "Leaves the current party.", format = "n", serverCommand = false)
	public boolean partyLeave(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);

		PartyChannel partyChannel = manager.getPartyChannel();
		if(partyChannel != null) {
			manager.leaveChannel(partyChannel);
			for(Player member : partyChannel.getPlayers())
				member.sendMessage(PARTY_MESSAGE_PREFIX + ChatColor.YELLOW + player.getDisplayName() + " left the party.");
			player.sendMessage(ChatColor.YELLOW + "You have left the party.");
		} else {
			player.sendMessage(ChatColor.RED + "You are not in a party.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.members", description = "Allows a user to list party members.", permissionDefault = PermissionDefault.TRUE)
	@CommandHandler(command = "party members", description = "List members in the current party.", format = "n", serverCommand = false)
	@CommandHandler(command = "party list", description = "List members in the current party.", format = "n", serverCommand = false)
	public boolean partyList(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(player);
		
		if(manager.getPartyChannel() != null) {
			String playerList = "";
			for(Player member : manager.getPartyChannel().getPlayers())
				playerList = playerList + member.getDisplayName() + ", ";
			
			player.sendMessage(ChatColor.YELLOW + "Party members are: " + playerList.substring(0, playerList.length() - 2) + ".");
		} else {
			player.sendMessage(ChatColor.RED + "You are not in a party.");
		}
		
		return true;
	}
	
	@PermissionHandler(node = "dotchat.party.info", description = "Allows a user to list other peoples party members.", permissionDefault = PermissionDefault.OP)
	@CommandHandler(command = "party info", description = "List members in the specified users party.", format = "p<User>", serverCommand = false)
	public boolean partyInfo(CommandEvent event) {
		Player player = (Player) event.getSender();
		PlayerChatManager manager = chatManager.getPlayerManager(Bukkit.getPlayer(event.getArgs()[0]));
		
		if(manager.getPartyChannel() != null) {
			String playerList = "";
			for(Player member : manager.getPartyChannel().getPlayers())
				playerList = playerList + member.getDisplayName() + ", ";
			
			player.sendMessage(ChatColor.YELLOW + "Players party members are: " + playerList.substring(0, playerList.length() - 2) + ".");
		} else {
			player.sendMessage(ChatColor.RED + "Player is not in a party.");
		}
		
		return true;
	}

	
	@EventHandler
	public void tabCompleteSwitch(TabCompleteEvent event) {
		String[] split = event.getBuffer().split(" ", -1);
		if(split.length < 3) {
			if(event.getBuffer().startsWith("/t") || event.getBuffer().startsWith("/tch")|| event.getBuffer().startsWith("/ch")) {
				Player player = (Player) event.getSender();
				PlayerChatManager playerManager = chatManager.getPlayerManager(player);
				
				if(split.length == 2) {
					event.setCompletions(playerManager.getChannelList(event.getBuffer().startsWith("/ch"), false, split[1]));
				} else {
					event.setCompletions(playerManager.getChannelList(event.getBuffer().startsWith("/ch"), false));
				}
			} else if(event.getBuffer().startsWith("/join")) {
				Player player = (Player) event.getSender();
				PlayerChatManager playerManager = chatManager.getPlayerManager(player);
				
				if(split.length == 2) {
					event.setCompletions(playerManager.getAvailableChannels(split[1]));
				} else {
					event.setCompletions(playerManager.getAvailableChannels());
				}
			} else if(event.getBuffer().startsWith("/leave") || event.getBuffer().startsWith("/close")) {
				Player player = (Player) event.getSender();
				PlayerChatManager playerManager = chatManager.getPlayerManager(player);
				
				if(split.length == 2) {
					event.setCompletions(playerManager.getChannelList(false, true, split[1]));
				} else {
					event.setCompletions(playerManager.getChannelList(false, true));
				}
			} else if(event.getBuffer().startsWith("/party")) {
				String typed = split.length == 2 ? split[1] : "" ;
				String[] partyCommands = new String[]{ "create", "invite", "join", "members", "list", "info" };
				List<String> completions = new ArrayList<String>();
				for(String cmd : partyCommands)
					if(cmd.startsWith(typed))
						completions.add(cmd);
				event.setCompletions(completions);
			}
		}
	}
}
