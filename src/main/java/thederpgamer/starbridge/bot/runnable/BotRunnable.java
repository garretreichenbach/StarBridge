package thederpgamer.starbridge.bot.runnable;

public interface BotRunnable extends Runnable {

	void start();
	void stop();
	RunnableStatus getStatus();
	void setStatus(RunnableStatus running);

	int getId();

	Exception getException();
}
