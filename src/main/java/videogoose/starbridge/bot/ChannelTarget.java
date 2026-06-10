package videogoose.starbridge.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.schema.common.util.StringTools;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.manager.ConfigManager;

import java.util.concurrent.TimeUnit;

/**
 * Routing targets for Discord messages.
 *
 * <p>CHAT sends to the configured {@code chat-channel-id}.<br>
 * LOG  sends to the configured {@code log-channel-id}.<br>
 * CHANGELOG sends to the configured {@code changelog-channel-id}.<br>
 * BOTH sends to both the chat and log channels.
 *
 * <p>All channel lookups are null-checked; a missing channel logs a warning
 * and returns cleanly rather than throwing NPE.
 *
 * @author VideoGoose (original), VideoGoose (rework)
 */
public enum ChannelTarget {
	CHAT,
	LOG,
	CHANGELOG,
	BOTH;

	/** Delay between queueing a message and it actually being sent, in ms. */
	private static final long QUEUE_DELAY_MS = 1000;

	public void sendMessage(JDA bot, String message) {
		message = StringTools.limit(message, 2000);
		StarBridge.getBot().checkReset();
		switch (this) {
			case CHAT -> sendToChannel(bot, message, "chat-channel-id");
			case LOG  -> sendToChannel(bot, message, "log-channel-id");
			case CHANGELOG -> sendToChannel(bot, message, "changelog-channel-id");
			case BOTH -> {
				sendToChannel(bot, message, "chat-channel-id");
				sendToChannel(bot, message, "log-channel-id");
			}
		}
	}

	// -------------------------------------------------------------------------
	// Private helper
	// -------------------------------------------------------------------------

	/**
	 * Looks up the TextChannel identified by {@code configKey}, null-checks it,
	 * and queues the message with a fixed delay.
	 */
	private static void sendToChannel(JDA bot, String message, String configKey) {
		long channelId = ConfigManager.getMainConfig().getLong(configKey);
		// Look up by GuildMessageChannel rather than TextChannel so announcement/news
		// channels (common for a changelog) are also resolved — getTextChannelById()
		// only matches plain text channels and returns null for a NewsChannel.
		GuildMessageChannel channel = bot.getChannelById(GuildMessageChannel.class, channelId);
		if (channel == null) {
			StarBridge.getInstance().logWarning(
					"ChannelTarget: channel for config key '" + configKey + "' (id=" + channelId + ") not found — message dropped");
			return;
		}
		// Capture effectively-final local for the lambda.
		final String finalMessage = message;
		try {
			channel.sendMessage(finalMessage).queueAfter(QUEUE_DELAY_MS, TimeUnit.MILLISECONDS);
		} catch (Exception exception) {
			StarBridge.getInstance().logException(
					"ChannelTarget: failed to queue message to " + configKey, exception);
		}
	}
}
