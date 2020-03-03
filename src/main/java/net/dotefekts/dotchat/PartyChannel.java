package net.dotefekts.dotchat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

public class PartyChannel extends ChatChannel {
	private List<Player> players;
	private List<UUID> invitedPlayers;
	
	public PartyChannel(int channelOrder, String displayName, String displayNameActive, boolean canLeave, boolean sendHistory) {
		super("party", channelOrder, displayName, displayNameActive, false, false, false, canLeave, true, sendHistory);
		
		players = new ArrayList<Player>();
		invitedPlayers = new ArrayList<UUID>();
	}
	
	public void addPlayer(Player player) {
		players.add(player);
		invitedPlayers.remove(player.getUniqueId());
	}
	
	public List<Player> getPlayers() {
		return new ArrayList<Player>(players);
	}
	
	public void removePlayer(Player player) {
		players.remove(player);
	}
	
	public void addInvitedPlayer(UUID playerUUID) {
		invitedPlayers.add(playerUUID);
	}
	
	public void removeInvitedPlayer(UUID playerUUID) {
		invitedPlayers.remove(playerUUID);
	}
	
	@Override
	public boolean canJoin(Player player) {
		return player.hasPermission("dotchat.party.forcejoin") || players.contains(player) || invitedPlayers.contains(player.getUniqueId());
	}
}
