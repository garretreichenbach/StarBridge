package thederpgamer.starbridge.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import thederpgamer.starbridge.StarBridge;
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

	public void sendMessage(JDA bot, Message message) {
		switch(this) {
			case CHAT:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).sendMessage(message).queue();
				} catch(Exception exception) {
					StarBridge.getInstance().logException("An exception occurred while sending Discord message", exception);
				}
				break;
			case LOG:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id")).sendMessage(message).queue();
				} catch(Exception exception) {
					StarBridge.getInstance().logException("An exception occurred while sending Discord message", exception);
				}
				break;
			case BOTH:
				try {
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).sendMessage(message).queue();
					bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id")).sendMessage(message).queue();
				} catch(Exception exception) {
					StarBridge.getInstance().logException("An exception occurred while sending Discord message", exception);
				}
				break;
		}
	}
}
