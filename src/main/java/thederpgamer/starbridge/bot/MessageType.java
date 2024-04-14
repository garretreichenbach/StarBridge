package thederpgamer.starbridge.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import thederpgamer.starbridge.StarBridge;

import java.util.Arrays;

/**
 * Enum used for defining different types of server messages from the bot.
 * <br/>Should not be used for user messages, just for things like server status updates.
 *
 * @author Garret Reichenbach
 */
public enum MessageType {
	PLAYER_KILL_EVENT(new MessageBuilder().append(":skull_crossbones: %s [%s] was killed by %s [%s]"), ChannelTarget.BOTH),
	PLAYER_DEATH_EVENT(new MessageBuilder().append(":skull: %s [%s] has died"), ChannelTarget.BOTH),
	PLAYER_SUICIDE_EVENT(new MessageBuilder().append(":skull: %s [%s] has taken their own life"), ChannelTarget.BOTH),
	FACTION_CREATE(new MessageBuilder().append(":shield: %s created a new faction %s"), ChannelTarget.BOTH),
	FACTION_DECLARE_WAR(new MessageBuilder().append(":crossed_swords: %s has declared war on %s"), ChannelTarget.BOTH),
	FACTION_PEACE(new MessageBuilder().append(":dove: %s has agreed to a peace deal with %s"), ChannelTarget.BOTH),
	FACTION_ALLY(new MessageBuilder().append(":shield: %s has agreed to an alliance with %s"), ChannelTarget.BOTH),
	FACTION_CANCEL_ALLY(new MessageBuilder().append(":x: %s has broken off from their alliance with %s"), ChannelTarget.BOTH),
	PLAYER_JOIN_FACTION(new MessageBuilder().append(":heavy_plus_sign: %s joined the faction %s"), ChannelTarget.BOTH),
	PLAYER_LEAVE_FACTION(new MessageBuilder().append(":heavy_minus_sign: %s left the faction %s"), ChannelTarget.BOTH),
	NEW_PLAYER_JOIN(new MessageBuilder().append(":tada: %s joined the server for the first time"), ChannelTarget.BOTH),
	PLAYER_JOIN(new MessageBuilder().append(":inbox_tray: %s joined the server"), ChannelTarget.BOTH),
	PLAYER_LEAVE(new MessageBuilder().append(":outbox_tray: %s left the server"), ChannelTarget.BOTH),
	SERVER_STARTING(new MessageBuilder().append(":white_check_mark: Server is starting..."), ChannelTarget.BOTH),
	SERVER_STARTED(new MessageBuilder().append(":white_check_mark: Server started in %dms"), ChannelTarget.BOTH),
	SERVER_STOPPING(new MessageBuilder().append(":stop_sign: Server is stopping..."), ChannelTarget.BOTH),
	SERVER_STOPPING_TIMED(new MessageBuilder().append(":stop_sign: Server will stop in %d seconds"), ChannelTarget.BOTH),
	SERVER_RESTARTING_TIMED(new MessageBuilder().append(":arrows_counterclockwise: Server will restart in %d seconds"), ChannelTarget.BOTH),
	SERVER_RESTARTING(new MessageBuilder().append(":arrows_counterclockwise: Server is restarting..."), ChannelTarget.BOTH),
	LOG_INFO(new MessageBuilder().append(":information_source: [INFO]:"), ChannelTarget.LOG),
	LOG_WARNING(new MessageBuilder().append(":warning: [WARNING]:"), ChannelTarget.LOG),
	LOG_EXCEPTION(new MessageBuilder().append(":exclamation: [EXCEPTION] Server encountered an exception:"), ChannelTarget.LOG),
	LOG_FATAL(new MessageBuilder().append(":bangbang: [FATAL]: Server crashed due to an exception:"), ChannelTarget.LOG);

	private final MessageBuilder builder;
	private final ChannelTarget target;

	MessageType(MessageBuilder builder, ChannelTarget target) {
		this.builder = builder;
		this.target = target;
	}

	public void sendMessage(Object... args) {
		JDA bot = StarBridge.getBot().getJDA();
		switch(this) {
			case PLAYER_KILL_EVENT:
				if(args != null && args.length == 4 && args[0] instanceof String && args[1] instanceof String && args[2] instanceof String && args[3] instanceof String) {
					String messageRaw = builder.getStringBuilder().toString();
					String message = String.format(messageRaw, args[0], args[1], args[2], args[3]);
					target.sendMessage(bot, new MessageBuilder().append(message).build());
				}
				break;
			case FACTION_CREATE:
			case FACTION_DECLARE_WAR:
			case FACTION_PEACE:
			case FACTION_ALLY:
			case FACTION_CANCEL_ALLY:
			case PLAYER_JOIN_FACTION:
			case PLAYER_LEAVE_FACTION:
			case PLAYER_DEATH_EVENT:
			case PLAYER_SUICIDE_EVENT:
				if(args != null && args.length == 2 && args[0] instanceof String && args[1] instanceof String) {
					String messageRaw = builder.getStringBuilder().toString();
					String message = String.format(messageRaw, args[0], args[1]);
					target.sendMessage(bot, new MessageBuilder().append(message).build());
				}
				break;
			case NEW_PLAYER_JOIN:
			case PLAYER_JOIN:
			case PLAYER_LEAVE:
				if(args != null && args.length == 1 && args[0] instanceof String) {
					String messageRaw = builder.getStringBuilder().toString();
					String message = String.format(messageRaw, args[0]);
					target.sendMessage(bot, new MessageBuilder().append(message).build());
				}
				break;
			case SERVER_STARTING:
			case SERVER_STOPPING:
			case SERVER_RESTARTING:
				target.sendMessage(bot, builder.build());
				break;
			case SERVER_STOPPING_TIMED:
			case SERVER_RESTARTING_TIMED:
				if(args != null && args.length == 1 && args[0] instanceof Integer) {
					String messageRaw = builder.getStringBuilder().toString();
					String message = String.format(messageRaw, args[0]);
					target.sendMessage(bot, new MessageBuilder().append(message).build());
				}
				break;
			case SERVER_STARTED:
				if(args != null && args.length == 1 && args[0] instanceof Long) {
					String messageRaw = builder.getStringBuilder().toString();
					String message = String.format(messageRaw, args[0]);
					target.sendMessage(bot, new MessageBuilder().append(message).build());
				}
				break;
			case LOG_INFO:
			case LOG_WARNING:
				if(args != null && args.length == 1 && args[0] instanceof String) builder.setEmbeds(new EmbedBuilder().setDescription((String) args[0]).build());
				break;
			case LOG_EXCEPTION:
			case LOG_FATAL:
				if(args != null && args.length == 2 && args[0] instanceof String && args[1] instanceof Exception) {
					Exception exception = (Exception) args[1];
					EmbedBuilder embed = new EmbedBuilder();
					embed.setTitle((String) args[0]);
					embed.setDescription(exception.getMessage());
					embed.addField("Exception", Arrays.toString(exception.getStackTrace()), false);
					target.sendMessage(bot, new MessageBuilder().setEmbeds(embed.build()).build());
				}
				break;
		}
	}
}
