package thederpgamer.starbridge.bot;

import net.dv8tion.jda.api.JDA;
import org.schema.common.util.StringTools;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.manager.ConfigManager;

import java.util.concurrent.TimeUnit;


public enum ChannelTarget {
	CHAT,
	LOG,
	BOTH;

	public void sendMessage(JDA bot, String message) {
		message = StringTools.limit(message, 2000);
		StarBridge.getBot().checkReset();
		switch(this) {
			case CHAT:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).sendMessage(message).queueAfter(1000, TimeUnit.MILLISECONDS);
				} catch(Exception exception) {
					exception.printStackTrace();
				}
				break;
			case LOG:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id")).sendMessage(message).queueAfter(1000, TimeUnit.MILLISECONDS);
				} catch(Exception exception) {
					exception.printStackTrace();
				}
				break;
			case BOTH:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).sendMessage(message).queueAfter(1000, TimeUnit.MILLISECONDS);
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id")).sendMessage(message).queueAfter(1000, TimeUnit.MILLISECONDS);
				} catch(Exception exception) {
					exception.printStackTrace();
				}
				break;
		}
	}
}
