package net.dotefekts.dotchat;

import com.comphenix.protocol.events.PacketContainer;

public class Message {
	private PacketContainer packet;
	private long time;
	
	public Message(PacketContainer packet, long time) {
		this.packet = packet;
		this.time = time;
	}
	
	public PacketContainer getPacket() {
		return packet.shallowClone();
	}
	
	public long getTime() {
		return time;
	}
}
