package net.dotefekts.dotchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;

public class Format {
	private static final String CHAT_SEPARATOR_DEFAULT = "§8==================================================";
	private static final String TAB_SEPARATOR_DEFAULT = "§8|";
	
	private static final String TAB_NAME_DEFAULT = " $n$u{§7(§e$u§7)}$t$c ";
	private static final String TAB_NAME_ACTIVE_DEFAULT = " $n$t$c ";
	private static final String TAB_NAME_CHATTING_DEFAULT = " [$n]$t$c ";
	
	private static final String TAB_TALK_DEFAULT = "§8[§7T§8]";
	private static final String TAB_CLOSE_DEFAULT = "§8[§cX§8]";
	
	private static final String ALL_NAME_DEFAULT = "All";
	private static final String ALL_NAME_ACTIVE_DEFAULT = "§lAll";
	private static final String ALL_SOURCE_NAME_DEFAULT = "§7[$n] ";
	
	private static final String PARTY_NAME_DEFAULT = "§dParty";
	private static final String PARTY_NAME_ACTIVE_DEFAULT = "§d§lParty";
	
	private static final String PM_NAME_DEFAULT = "§b$u";
	private static final String PM_NAME_ACTIVE_DEFAULT = "§b§l$u";
	
	private String legacyChatSeparator;
	private String legacyTabSeparator;
	
	private String legacyTabName;
	private String legacyTabNameActive;
	private String legacyTabNameChatting;

	private String legacyTabTalk;
	private String legacyTabClose;
	
	private String legacyAllName;
	private String legacyAllNameActive;
	private String legacyAllSourceName;
	
	private String legacyPartyName;
	private String legacyPartyNameActive;
	
	private String legacyPmName;
	private String legacyPmNameActive;
	
	public Format(ConfigurationSection config) {
		if(config == null) {
			legacyChatSeparator = CHAT_SEPARATOR_DEFAULT;
			legacyTabSeparator = TAB_SEPARATOR_DEFAULT;
			
			legacyTabName = TAB_NAME_DEFAULT;
			legacyTabNameActive = TAB_NAME_ACTIVE_DEFAULT;
			legacyTabNameChatting = TAB_NAME_CHATTING_DEFAULT;
			
			legacyTabTalk = TAB_TALK_DEFAULT;
			legacyTabClose = TAB_CLOSE_DEFAULT;
			
			legacyAllName = ALL_NAME_DEFAULT;
			legacyAllNameActive = ALL_NAME_ACTIVE_DEFAULT;
			legacyAllSourceName = ALL_SOURCE_NAME_DEFAULT;
			
			legacyPartyName = ALL_NAME_ACTIVE_DEFAULT;
			legacyPartyNameActive = ALL_SOURCE_NAME_DEFAULT;
			
			legacyPmName = PM_NAME_DEFAULT;
			legacyPmNameActive = PM_NAME_ACTIVE_DEFAULT;
		} else {
			legacyChatSeparator = config.getString("chat-separator", CHAT_SEPARATOR_DEFAULT);
			legacyTabSeparator = config.getString("tab-separator", TAB_SEPARATOR_DEFAULT);
			
			legacyTabName = config.getString("tab-name", TAB_NAME_DEFAULT);
			legacyTabNameActive = config.getString("tab-name-active", TAB_NAME_ACTIVE_DEFAULT);
			legacyTabNameChatting = config.getString("tab-name-chatting", TAB_NAME_CHATTING_DEFAULT);
			
			legacyTabTalk = config.getString("tab-talk", TAB_TALK_DEFAULT);
			legacyTabClose = config.getString("tab-close", TAB_CLOSE_DEFAULT);
			
			legacyAllName = config.getString("all-channel-name", ALL_NAME_DEFAULT);
			legacyAllNameActive = config.getString("all-channel-name-active", ALL_NAME_ACTIVE_DEFAULT);
			legacyAllSourceName = config.getString("all-source-name", ALL_SOURCE_NAME_DEFAULT);
			
			legacyPartyName = config.getString("party-channel-name", PARTY_NAME_DEFAULT);
			legacyPartyNameActive = config.getString("party-channel-name-active", PARTY_NAME_ACTIVE_DEFAULT);
			
			legacyPmName = config.getString("pm-channel-name", PM_NAME_DEFAULT);
			legacyPmNameActive = config.getString("pm-channel-name-active", PM_NAME_ACTIVE_DEFAULT);
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
	
	public BaseComponent[] getTabName(ChatChannel channel, ChatType type, boolean showTalkSwitch, int unread) {
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
		
		if(!channel.canTalk()) {
			builtText.replace("$t", "");
		}
		
		if(!channel.canLeave()) {
			builtText.replace("$c", "");
		}
		
		List<BaseComponent> components = new ArrayList<BaseComponent>();
		if(builtText.contains("$t") || builtText.contains("$c")) {
			int nextIndex = -1;
			do {
				nextIndex = Math.min(builtText.indexOf("$t"), builtText.indexOf("$c"));
				if(nextIndex == -1)
					nextIndex = Math.max(builtText.indexOf("$t"), builtText.indexOf("$c"));
				
				if(nextIndex != -1) {
					List<BaseComponent> nextComponents = new ArrayList<BaseComponent>(Arrays.asList(TextComponent.fromLegacyText(builtText.substring(0, nextIndex))));
					for(BaseComponent c : nextComponents)
						c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/ch " + channel.getName()));
					
					if(builtText.substring(nextIndex, nextIndex + 2).equals("$t") && showTalkSwitch && type == ChatType.INACTIVE && channel.canTalk()) {
						BaseComponent[] component = TextComponent.fromLegacyText(legacyTabTalk);
						for(BaseComponent c : component)
							c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/tch " + channel.getName()));
						
						nextComponents.addAll(Arrays.asList(component));
					} else if(builtText.substring(nextIndex, nextIndex + 2).equals("$c") && channel.canLeave()) {
						BaseComponent[] component = TextComponent.fromLegacyText(legacyTabClose);
						for(BaseComponent c : component)
							c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/close " + channel.getName()));
						
						nextComponents.addAll(Arrays.asList(component));
					}
					
					components.addAll(nextComponents);
					builtText = builtText.substring(nextIndex + 2);
				}
				
			} while(nextIndex != -1);
		}
		

		List<BaseComponent> nextComponents = new ArrayList<BaseComponent>(Arrays.asList(TextComponent.fromLegacyText(builtText)));
		for(BaseComponent c : nextComponents)
			c.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/ch " + channel.getName()));
		components.addAll(nextComponents);
		
		return components.toArray(new BaseComponent[components.size()]);
	}
	
	public String getAllTabName() {
		return legacyAllName;
	}
	
	public String getAllTabNameActive() {
		return legacyAllNameActive;
	}
	
	public String getAllSourceName(ChatChannel channel) {
		String builtText = legacyAllSourceName;
		builtText = builtText.replace("$n", channel.getDisplayName(false));
		builtText = builtText.replace("$a", channel.getDisplayName(true));
		
		return builtText;
	}

	public String getPartyTabName() {
		return legacyPartyName;
	}

	public String getPartyTabNameActive() {
		return legacyPartyNameActive;
	}

	public String getPmTabName(Player player) {
		return legacyPmName.replace("$u", player.getDisplayName());
	}

	public String getPmTabNameActive(Player player) {
		return legacyPmNameActive.replace("$u", player.getDisplayName());
	}
	
	public enum ChatType {
		INACTIVE,
		ACTIVE,
		CHATTING
	}
}
