package thederpgamer.starbridge.bot.runnable;

import api.common.GameServer;
import api.mod.config.FileConfiguration;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.schema.game.server.data.ServerConfig;
import thederpgamer.starbridge.bot.BotLogger;
import thederpgamer.starbridge.bot.StarBot;
import thederpgamer.starbridge.commands.*;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.LogManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles bot runners.
 */
public class BotThread extends Thread {

	private boolean running = true;
	private final ConcurrentHashMap<BotRunnable, Long> runners = new ConcurrentHashMap<>();
	public static int idCount = 0;
	public JDA bot;
	private final long startTime = System.currentTimeMillis();
	private final long restartTime;
	public String lastMessage = "";
	private boolean firstWarning = false;
	private boolean secondWarning = false;
	private boolean thirdWarning = false;
	private BotLogger botLogger;

	public BotThread(FileConfiguration config, StarBot instance) {
		JDABuilder builder = JDABuilder.createDefault(config.getString("bot-token"));
		builder.setActivity(Activity.playing("StarMade"));
		builder.addEventListeners(instance);
		bot = builder.build();
		LogManager.logInfo("Successfully initialized bot.");
		registerCommands();
		(new Timer("channel_updater")).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				updateChannelInfo();
			}
		}, 0, 10000);
		restartTime = config.getLong("restart-timer") + System.currentTimeMillis();
		botLogger = new BotLogger();
		botLogger.startWatch();
	}

	@Override
	public void run() {
		while(running) {
			for(Map.Entry<BotRunnable, Long> entry : runners.entrySet()) {
				RunnableStatus status = entry.getKey().getStatus();
				switch(status) {
					case WAITING:
						entry.getKey().start();
						entry.getKey().setStatus(RunnableStatus.RUNNING);
						break;
					case RUNNING:
						if(entry.getValue() > ConfigManager.getMainConfig().getConfigurableLong("max-runner-time", 10000)) {
							entry.getKey().stop();
							entry.getKey().setStatus(RunnableStatus.STOPPED);
							LogManager.logWarning("Runner " + entry.getKey().getClass().getSimpleName() + "[" + entry.getKey().getId() + "] has been stopped due to timeout.", null);
							runners.remove(entry.getKey());
						} else entry.setValue(entry.getValue() + (System.currentTimeMillis() - entry.getValue()));
						break;
					case STOPPED:
						//Check for error, if one is found report it to the console. If not, remove it from the list.
						if(entry.getKey().getStatus() == RunnableStatus.ERROR) {
							LogManager.logException("Runner " + entry.getKey().getClass().getSimpleName() + "[" + entry.getKey().getId() + "] has been stopped due to an error.", entry.getKey().getException());
							runners.remove(entry.getKey());
						} else runners.remove(entry.getKey());
						break;
					case ERROR:
						LogManager.logException("Runner " + entry.getKey().getClass().getSimpleName() + "[" + entry.getKey().getId() + "] has been stopped due to an error.", entry.getKey().getException());
						runners.remove(entry.getKey());
						break;
				}
			}

			long currentTime = System.currentTimeMillis();
			if(restartTime - currentTime <= 900000 && !firstWarning) { //15 minute warning
				GameServer.getServerState().executeAdminCommand(null, "shutdown " + (ConfigManager.getMainConfig().getInt("default-shutdown-timer") / 1000) + 5, GameServer.getServerState().getAdminLocalClient());
				StarBot.getInstance().sendDiscordMessage(":warning: Server will restart in 15 minutes");
				StarBot.getInstance().sendServerMessage("Server will restart in 15 minutes");
				firstWarning = true;
			} else if(restartTime - currentTime <= 300000 && !secondWarning) { //5 minute warning
				StarBot.getInstance().sendDiscordMessage(":warning: Server will restart in 5 minutes");
				StarBot.getInstance().sendServerMessage("Server will restart in 5 minutes");
				secondWarning = true;
			} else if(restartTime - currentTime <= 60000 && !thirdWarning) { //1 minute warning
				StarBot.getInstance().sendDiscordMessage(":warning: Server will restart in 1 minute");
				StarBot.getInstance().sendServerMessage("Server will restart in 1 minute");
				thirdWarning = true;
			} else if(restartTime <= currentTime) running = false;
		}

		LogManager.logInfo("Bot thread shutting down.");
		StarBot.getInstance().sendDiscordMessage(":stop_sign: Server is restarting.");
		StarBot.getInstance().sendServerMessage("Server is restarting.");
		//Stop all runners and send bot shutdown message.
		try {
			for(BotRunnable runner : runners.keySet()) runner.stop();
		} catch(Exception ignored) {} //If the runners fail to finish, it's probably a good idea to just ignore it.
	}

	public void queue(BotRunnable runner) {
		runners.put(runner, System.currentTimeMillis());
	}

	private void registerCommands() {
		CommandInterface[] commandArray = new CommandInterface[] {
				new ListCommand(),
				new LinkCommand(),
				new ClearDataCommand(),
				new InfoPlayerCommand(),
				new InfoFactionCommand(),
				new HelpDiscordCommand()
		};
		CommandListUpdateAction commands = bot.updateCommands();

		ArrayList<CommandInterface> commandList = new ArrayList<>(Arrays.asList(commandArray));
		//addAPICommands(commandList);

		for(CommandInterface commandInterface : commandList) {
			commands.addCommands(((DiscordCommand) commandInterface).getCommandData()).queue();
			LogManager.logInfo( "Registered command /" + commandInterface.getCommand());
		}
		commands.queue();
	}

	public void updateChannelInfo() {
		try {
			int playerCount = GameServer.getServerState().getPlayerStatesByName().size();
			int playerMax = (int) ServerConfig.MAX_CLIENTS.getCurrentState();

			String chatChannelStats = ("Players: " + playerCount + " / " + playerMax
					//"Next Restart: " + ServerUtils.getNextRestart()
			);
			Objects.requireNonNull(bot.getTextChannelById(StarBot.getInstance().getChatChannelId())).getManager().setTopic(chatChannelStats).queue();
			String logChannelStats = ("Clients: " + playerCount + " / " + playerMax  + " \nCurrent Uptime: " + (System.currentTimeMillis() - startTime));
			Objects.requireNonNull(bot.getTextChannelById(StarBot.getInstance().getLogChannelId())).getManager().setTopic(logChannelStats).queue();
		} catch(Exception exception) {
			LogManager.logException("Failed to update channel info", exception);
		}
	}

}
