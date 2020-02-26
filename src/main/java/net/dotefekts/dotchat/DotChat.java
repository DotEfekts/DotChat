package net.dotefekts.dotchat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import net.dotefekts.dotutils.DotUtilities;

public class DotChat extends JavaPlugin {
	private ProtocolManager protocolManager;
	private ChatManager chatManager;
	private ChatReplayManager chatReplayManager;
	private CommandListener commandListener;
	
    @Override
    public void onEnable() {
    	this.saveDefaultConfig();
    	
    	protocolManager = ProtocolLibrary.getProtocolManager();
    	chatManager = new ChatManager(this, protocolManager);
    	chatReplayManager = new ChatReplayManager(this, protocolManager, chatManager);
    	commandListener = new CommandListener(chatManager);
    	
    	Bukkit.getPluginManager().registerEvents(chatReplayManager, this);
    	DotUtilities.getCommandHelper().registerCommands(commandListener, this);
    	
    	getLogger().info("DotChat has finished loading.");
    }

    @Override
    public void onDisable() {    	
    	getLogger().info("DotChat has finished disabling.");
    }
}
