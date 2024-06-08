package thederpgamer.starbridge.bot;

import net.dv8tion.jda.api.JDA;
import thederpgamer.starbridge.manager.ConfigManager;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public enum ChannelTarget {
	CHAT,
	LOG,
	BOTH;

	public void sendMessage(JDA bot, String message) {
		switch(this) {
			case CHAT:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).sendMessage(message).queue();
				} catch(Exception exception) {
					exception.printStackTrace();
				}
				break;
			case LOG:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id")).sendMessage(message).queue();
				} catch(Exception exception) {
					exception.printStackTrace();
				}
				break;
			case BOTH:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).sendMessage(message).queue();
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id")).sendMessage(message).queue();
				} catch(Exception exception) {
					exception.printStackTrace();
				}
				break;
		}
	}
}
