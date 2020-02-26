package net.dotefekts.dotchat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;

class ChatUtilities {
	static PacketContainer buildChatPacket(String message, boolean isJson, boolean isSystem) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
		packet.getChatComponents().write(0, isJson ? WrappedChatComponent.fromJson(message) : WrappedChatComponent.fromText(message));
		packet.getChatTypes().write(0, isSystem ? ChatType.SYSTEM : ChatType.CHAT);
		
		return packet;
	}
}
