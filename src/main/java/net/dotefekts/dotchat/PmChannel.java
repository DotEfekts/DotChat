package net.dotefekts.dotchat;

import org.bukkit.entity.Player;

public class PmChannel extends ChatChannel {
	private Player owningPlayer;
	private Player partnerPlayer;
	
	public PmChannel(Player owningPlayer, Player partnerPlayer, int channelOrder, String displayName, String displayNameActive, boolean sendHistory) {
		super("p:" + owningPlayer.getName() + ";" + partnerPlayer.getName(), channelOrder, displayName, displayNameActive, false, false, false, true, true, sendHistory);
		
		this.owningPlayer = owningPlayer;
		this.partnerPlayer = partnerPlayer;
	}
	
	public Player getOwner() {
		return owningPlayer;
	}
	
	public Player getPartner() {
		return partnerPlayer;
	}
	
	public boolean isInChat(Player player) {
		return player == owningPlayer || player == partnerPlayer;
	}
}
