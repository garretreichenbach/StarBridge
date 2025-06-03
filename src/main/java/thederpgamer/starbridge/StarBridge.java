package thederpgamer.starbridge;

import api.listener.events.Event;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import org.schema.schine.network.server.ServerState;
import thederpgamer.starbridge.bot.DiscordBot;
import thederpgamer.starbridge.bot.MessageType;
import thederpgamer.starbridge.commands.*;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.EventManager;

/**
 * Main mod class for StarBridge.
 */
public class StarBridge extends StarMod {
	private static StarBridge instance;
	private static DiscordBot bot;

	public StarBridge() {
		instance = this;
	}

	//Instance
	public static void main(String[] args) {
	}

	public static StarBridge getInstance() {
		return instance;
	}

	public static DiscordBot getBot() {
		return bot;
	}

	@Override
	public void onEnable() {
		long start = System.currentTimeMillis();
		instance = this;
		addShutdownHook();
		ConfigManager.initialize();
		EventManager.initialize(this);
		registerCommands();
		bot = DiscordBot.initialize(this);
		long took = System.currentTimeMillis() - start;
		MessageType.SERVER_STARTED.sendMessage(took);
		if(ConfigManager.getMainConfig().getBoolean("debug-mode")) MessageType.DEBUG_MODE_STARTED.sendMessage();
	}

	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if(!ServerState.isShutdown()) MessageType.LOG_FATAL.sendMessage("Server has shutdown unexpectedly due to a fatal error!", null);
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}));
	}

	@Override
	public void onDisable() {
		super.logInfo("Server Stopping...");
//		MessageType.SERVER_STOPPING.sendMessage();
	}

	@Override
	public void logInfo(String message) {
		super.logInfo(message);
		MessageType.LOG_INFO.sendMessage(message);
	}

	@Override
	public void logWarning(String message) {
		super.logWarning(message);
		MessageType.LOG_WARNING.sendMessage(message);
	}

	@Override
	public void logException(String message, Exception exception) {
		super.logException(message, exception);
		exception.printStackTrace();
		MessageType.LOG_EXCEPTION.sendMessage(message, exception);
	}

	private void registerCommands() {
		CommandInterface[] commands = {new ListCommand(), new LinkCommand(), new InfoPlayerCommand(), new InfoFactionCommand(), new RenameSystemCommand()};
		for(CommandInterface commandInterface : commands) StarLoader.registerCommand(commandInterface);
	}

	public void handleEvent(Event event) {
		bot.handleEvent(event);
	}
}
