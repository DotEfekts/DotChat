package net.dotefekts.dotchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

class ChatUtilities {
	static PacketContainer buildChatPacket(String message, boolean isJson, boolean isSystem) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
		packet.getChatComponents().write(0, isJson ? WrappedChatComponent.fromJson(message) : WrappedChatComponent.fromText(message));
		packet.getChatTypes().write(0, isSystem ? ChatType.SYSTEM : ChatType.CHAT);
		
		return packet;
	}
	
	static PacketContainer addChannelPrefix(PacketContainer message, String sourceChannelName) {
		PacketContainer multiMessage = message.shallowClone();
		WrappedChatComponent chatComponent = multiMessage.getChatComponents().readSafely(0);
		List<BaseComponent> messageComponents = chatComponent != null?
				new ArrayList<BaseComponent>(Arrays.asList(ComponentSerializer.parse(chatComponent.getJson())))
				: new ArrayList<BaseComponent>(Arrays.asList(TextComponent.fromLegacyText("")));
		messageComponents.addAll(0, Arrays.asList(TextComponent.fromLegacyText(sourceChannelName)));
		chatComponent.setJson(ComponentSerializer.toString(messageComponents.toArray(new BaseComponent[messageComponents.size()])));
		
		multiMessage.getChatComponents().write(0, chatComponent);
		
		return multiMessage;
	}
}
