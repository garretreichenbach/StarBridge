package thederpgamer.starbridge.bot.runnable;

import thederpgamer.starbridge.bot.Bot;
import thederpgamer.starbridge.bot.BotThread;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.server.ServerDatabase;

public class DiscordMessageRunnable implements BotRunnable {

	private Exception exception = null;
	private final int id = BotThread.idCount ++;
	private RunnableStatus status;
	private String sender;
	private String message;

	public DiscordMessageRunnable(String message) {
		this.message = message;
		this.status = RunnableStatus.WAITING;
		sanitizeMessage();
	}

	public DiscordMessageRunnable(String sender, String message) {
		this.sender = sender;
		this.message = message;
		this.status = RunnableStatus.WAITING;
		sanitizeMessage();
	}

	@Override
	public void start() {
		status = RunnableStatus.RUNNING;
		run();
	}

	@Override
	public void stop() {
		status = RunnableStatus.STOPPED;
	}

	@Override
	public void run() {
		status = RunnableStatus.RUNNING;
		try {
			PlayerData playerData = ServerDatabase.getPlayerDataWithoutNull(sender);
			Bot.getInstance().resetWebhook();

			if(playerData.getDiscordId() != -1) Bot.getInstance().getChatWebhook().setAvatarUrl(Bot.getInstance().getBotThread().bot.retrieveUserById(playerData.getDiscordId()).complete(true).getEffectiveAvatarUrl());
			else Bot.getInstance().getChatWebhook().setAvatarUrl(ConfigManager.getMainConfig().getString("bot-avatar"));

			if(playerData.inFaction()) Bot.getInstance().getChatWebhook().setUsername(playerData.getPlayerName() + "[" + playerData.getFactionName() + "]");
			else Bot.getInstance().getChatWebhook().setUsername(playerData.getPlayerName());

			Bot.getInstance().getChatWebhook().setContent(message);
			Bot.getInstance().getChatWebhook().execute();
			Bot.getInstance().resetWebhook();
		} catch(Exception e) {
			exception = e;
			status = RunnableStatus.ERROR;
			return;
		}
		status = RunnableStatus.STOPPED;
	}

	@Override
	public RunnableStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(RunnableStatus running) {
		this.status = status;
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public Exception getException() {
		return exception;
	}

	private void sanitizeMessage() {
		message = message.replace("@", "");
		message = message.replace("\"", "");
	}
}
