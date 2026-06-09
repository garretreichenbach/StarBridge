package videogoose.starbridge;

import api.listener.events.Event;
import api.mod.StarMod;
import videogoose.starbridge.bot.DiscordBot;
import videogoose.starbridge.bot.MessageType;
import videogoose.starbridge.commands.CommandTypes;
import videogoose.starbridge.error.ErrorManager;
import videogoose.starbridge.manager.ConfigManager;
import videogoose.starbridge.manager.EventManager;
import videogoose.starbridge.server.ChangelogTracker;

import java.util.concurrent.atomic.AtomicBoolean;

public class StarBridge extends StarMod {
	private static StarBridge instance;
	private static DiscordBot bot;
	private final AtomicBoolean disabled = new AtomicBoolean(false);

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
		ErrorManager.initialize();
		EventManager.initialize(this);
		bot = DiscordBot.initialize(this);
		long took = System.currentTimeMillis() - start;
		CommandTypes.registerGameCommands();
		MessageType.SERVER_STARTED.sendMessage(took);
		ChangelogTracker.checkAndPost();
		if(ConfigManager.getMainConfig().getBoolean("debug-mode")) {
			MessageType.DEBUG_MODE_STARTED.sendMessage();
		}
	}

	private void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				onDisable();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}));
	}

	@Override
	public void onDisable() {
		// Runs at most once, whether triggered by the mod lifecycle or the JVM shutdown hook.
		// Crashes are reported via ServerCrashEvent and the exception stream watcher, so this
		// no longer guesses "unexpected shutdown" (which false-alarmed on clean exits).
		if(!disabled.compareAndSet(false, true)) return;
		super.logInfo("Server Stopping...");
		MessageType.SERVER_STOPPING.sendMessage();
		if(bot != null) bot.shutdown();
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
		// Route through the registry for dedup/rate-limit/mute instead of posting directly.
		ErrorManager.report(message, exception);
	}

	public void handleEvent(Event event) {
		bot.handleEvent(event);
	}
}
