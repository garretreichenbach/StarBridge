package thederpgamer.starbridge;

import api.ModPlayground;
import api.listener.events.Event;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import api.utils.other.HashList;
import org.schema.schine.network.server.ServerState;
import thederpgamer.starbridge.bot.DiscordBot;
import thederpgamer.starbridge.bot.MessageType;
import thederpgamer.starbridge.commands.*;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.EventManager;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main mod class for StarBridge.
 */
public class StarBridge extends StarMod {
	//Instance
	public static void main(String[] args) { }
	private static StarBridge instance;
	public static StarBridge getInstance() {
		return instance;
	}
	public StarBridge() {
		instance = this;
	}
	private static DiscordBot bot;

	@Override
	public void onEnable() {
		long start = System.currentTimeMillis();
		instance = this;
		addShutdownHook();
		ConfigManager.initialize();
		doOverwrites();
		EventManager.initialize(this);
		registerCommands();
		bot = DiscordBot.initialize(this);
		(new Timer("channel_updater")).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				bot.updateChannelInfo();
			}
		}, 500, 5000);
		long took = System.currentTimeMillis() - start;
		MessageType.SERVER_STARTED.sendMessage(took);
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
		MessageType.SERVER_STOPPING.sendMessage();
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
		MessageType.LOG_EXCEPTION.sendMessage(message, exception);
	}

	private void doOverwrites() {
		try {
			Field commandsField = StarLoader.class.getDeclaredField("commands");
			commandsField.setAccessible(true);
			HashList<StarMod, CommandInterface> commands = (HashList<StarMod, CommandInterface>) commandsField.get(null);
			commands.getList(ModPlayground.inst).remove(StarLoader.getCommand("help"));
			commands.getList(ModPlayground.inst).add(new HelpDiscordCommand());
			commandsField.set(null, commands);
		} catch(NoSuchFieldException | IllegalAccessException | ClassCastException exception) {
			exception.printStackTrace();
		}
	}

	private void registerCommands() {
		CommandInterface[] commands = {
				new ListCommand(),
				new LinkCommand(),
				new InfoPlayerCommand(),
				new InfoFactionCommand(),
				new RenameSystemCommand()
		};
		for(CommandInterface commandInterface : commands) StarLoader.registerCommand(commandInterface);
	}

	public static DiscordBot getBot() {
		return bot;
	}

	public void handleEvent(Event event) {
		bot.handleEvent(event);
	}
}
