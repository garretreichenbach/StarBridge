package thederpgamer.starbridge.bot.runnable;

import api.common.GameServer;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.RegisteredClientOnServer;
import thederpgamer.starbridge.bot.Bot;
import thederpgamer.starbridge.bot.BotThread;

public class ServerMessageRunnable implements BotRunnable {

	private Exception exception = null;
	private final int id = BotThread.idCount ++;
	private RunnableStatus status;
	private String sender;
	private String message;

	public ServerMessageRunnable(String message) {
		this.message = message;
		this.status = RunnableStatus.WAITING;
		sanitizeMessage();
	}

	public ServerMessageRunnable(String sender, String message) {
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
			if(GameServer.getServerState() == null) return;
			if(Bot.getInstance().getBotThread().lastMessage.trim().toLowerCase().equals(message.toLowerCase().trim())) {
				status = RunnableStatus.STOPPED;
				return;
			}

			for(RegisteredClientOnServer client : GameServerState.instance.getClients().values()) {
				if(sender != null) client.serverMessage("[" + sender + "] " + message);
				else client.serverMessage(message);
			}

			Bot.getInstance().getBotThread().lastMessage = message;
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
