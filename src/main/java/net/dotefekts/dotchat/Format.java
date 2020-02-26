package net.dotefekts.dotchat;

import org.bukkit.configuration.ConfigurationSection;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class Format {
	private static final String CHAT_SEPARATOR_DEFAULT = "§8==================================================";
	private static final String TAB_SEPARATOR_DEFAULT = "§8|";
	private static final String TAB_NAME_DEFAULT = " $n$u{§7(§e$u§7)} ";
	private static final String TAB_NAME_ACTIVE_DEFAULT = " $n ";
	private static final String TAB_NAME_CHATTING_DEFAULT = " [$n] ";
	private static final String ALL_NAME_DEFAULT = "All";
	private static final String ALL_NAME_ACTIVE_DEFAULT = "§lAll";
	private static final String ALL_SOURCE_NAME_DEFAULT = "§7[$n] ";
	
	private String legacyChatSeparator;
	private String legacyTabSeparator;
	
	private String legacyTabName;
	private String legacyTabNameActive;
	private String legacyTabNameChatting;
	
	private String legacyAllName;
	private String legacyAllNameActive;
	private String legacyAllSourceName;
	
	public Format(ConfigurationSection config) {
		if(config == null) {
			legacyChatSeparator = CHAT_SEPARATOR_DEFAULT;
			legacyTabSeparator = TAB_SEPARATOR_DEFAULT;
			
			legacyTabName = TAB_NAME_DEFAULT;
			legacyTabNameActive = TAB_NAME_ACTIVE_DEFAULT;
			
			legacyAllName = ALL_NAME_DEFAULT;
			legacyAllNameActive = ALL_NAME_ACTIVE_DEFAULT;
			legacyAllSourceName = ALL_SOURCE_NAME_DEFAULT;
		} else {
			legacyChatSeparator = config.getString("chat-separator", CHAT_SEPARATOR_DEFAULT);
			legacyTabSeparator = config.getString("tab-separator", TAB_SEPARATOR_DEFAULT);
			
			legacyTabName = config.getString("tab-name", TAB_NAME_DEFAULT);
			legacyTabNameActive = config.getString("tab-name-active", TAB_NAME_ACTIVE_DEFAULT);
			legacyTabNameChatting = config.getString("tab-name-chatting", TAB_NAME_CHATTING_DEFAULT);
			
			legacyAllName = config.getString("all-channel-name", ALL_NAME_DEFAULT);
			legacyAllNameActive = config.getString("all-channel-name-active", ALL_NAME_ACTIVE_DEFAULT);
			legacyAllSourceName = config.getString("all-source-name", ALL_SOURCE_NAME_DEFAULT);
		}
	}
	
	public BaseComponent[] getChatSeparator() {
		if(legacyChatSeparator.isEmpty())
			return null;
		return TextComponent.fromLegacyText(legacyChatSeparator);
	}
	
	public BaseComponent[] getTabSeparator() {
		if(legacyChatSeparator.isEmpty())
			return null;
		return TextComponent.fromLegacyText(legacyTabSeparator);
	}
	
	public BaseComponent[] getTabName(ChatChannel channel, ChatType type, int unread) {
		String builtText;
		
		switch(type) {
			case ACTIVE:
				builtText = legacyTabNameActive;
				break;
			case CHATTING:
				builtText = legacyTabNameChatting;
				break;
			case INACTIVE:
			default:
				builtText = legacyTabName;
				break;
		}
		
		builtText = builtText.replace("$n", channel.getDisplayName(false));
		builtText = builtText.replace("$a", channel.getDisplayName(true));
		
		if(unread > 0) {
			builtText = builtText.replaceFirst("\\$u\\{([^\\}]*)\\}", "$1").replace("$u", Integer.toString(unread));
		} else if(unread <= 0) {
			builtText = builtText.replaceFirst("\\$u\\{([^\\}]*)\\}", "");
		}
		
		return TextComponent.fromLegacyText(builtText);
	}
	
	public String getAllTabName() {
		return legacyAllName;
	}
	
	public String getAllTabActiveName() {
		return legacyAllNameActive;
	}
	
	public String getAllSourceName(String tabName) {
		return legacyAllSourceName.replace("$n", tabName);
	}
	
	public enum ChatType {
		INACTIVE,
		ACTIVE,
		CHATTING
	}
}
