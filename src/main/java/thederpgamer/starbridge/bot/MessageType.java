package thederpgamer.starbridge.bot;

import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.schema.common.util.StringTools;
import thederpgamer.starbridge.StarBridge;

import java.util.Arrays;

/**
 * Enum used for defining different types of server messages from the bot.
 * <br/>Should not be used for user messages, just for things like server status updates.
 *
 * @author Garret Reichenbach
 */
public enum MessageType {
	PLAYER_KILL_EVENT(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":skull_crossbones:") + " %s [%s] was killed by %s").build(), ChannelTarget.BOTH),
	PLAYER_DEATH_EVENT(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":skull:") + " %s [%s] has died").build(), ChannelTarget.BOTH),
	PLAYER_SUICIDE_EVENT(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":skull:") + " s [%s] has taken their own life").build(), ChannelTarget.BOTH),
	FACTION_CREATE(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":shield:") + " %s created a new faction %s").build(), ChannelTarget.BOTH),
	FACTION_DECLARE_WAR(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":crossed_swords:") + " %s has declared war on %s").build(), ChannelTarget.BOTH),
	FACTION_PEACE(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":dove:") + " %s has agreed to a peace deal with %s").build(), ChannelTarget.BOTH),
	FACTION_ALLY(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":shield:") + " %s has agreed to an alliance with %s").build(), ChannelTarget.BOTH),
	FACTION_CANCEL_ALLY(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":x:") + " %s has broken off from their alliance with %s").build(), ChannelTarget.BOTH),
	PLAYER_JOIN_FACTION(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":heavy_plus_sign:") + " %s joined the faction %s").build(), ChannelTarget.BOTH),
	PLAYER_LEAVE_FACTION(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":heavy_minus_sign:") + " %s left the faction %s").build(), ChannelTarget.BOTH),
	NEW_PLAYER_JOIN(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":tada:") + " %s joined the server for the first time").build(), ChannelTarget.BOTH),
	PLAYER_JOIN(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":inbox_tray:") + " %s joined the server").build(), ChannelTarget.BOTH),
	PLAYER_LEAVE(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":outbox_tray:") + " %s left the server").build(), ChannelTarget.BOTH),
	SERVER_STARTING(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":white_check_mark:") + " Server is starting...").build(), ChannelTarget.BOTH),
	SERVER_STARTED(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":white_check_mark:") + " Server started in %dms").build(), ChannelTarget.BOTH),
	SERVER_STOPPING(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":stop_sign:") + " Server is stopping...").build(), ChannelTarget.BOTH),
	SERVER_STOPPING_TIMED(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":stop_sign:") + " Server will stop in %d seconds").build(), ChannelTarget.BOTH),
	SERVER_RESTARTING_TIMED(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":arrows_counterclockwise:") + " Server will restart in %d seconds").build(), ChannelTarget.BOTH),
	SERVER_RESTARTING(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":arrows_counterclockwise:") + " Server is restarting...").build(), ChannelTarget.BOTH),
	LOG_INFO(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":information_source:") + " [INFO]:").build(), ChannelTarget.LOG),
	LOG_WARNING(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":warning:") + " [WARNING]:").build(), ChannelTarget.LOG),
	LOG_EXCEPTION(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":exclamation:") + " [EXCEPTION] Server encountered an exception:").build(), ChannelTarget.LOG),
	LOG_FATAL(new MessageCreateBuilder().addContent(EmojiParser.parseToUnicode(":bangbang:") + " [FATAL]: Server crashed due to an exception:").build(), ChannelTarget.LOG);

	private final MessageCreateData builder;
	private final ChannelTarget target;

	MessageType(MessageCreateData builder, ChannelTarget target) {
		this.builder = builder;
		this.target = target;
	}

	public void sendMessage(Object... args) {
		JDA bot = StarBridge.getBot().getJDA();
		switch(this) {
			case PLAYER_KILL_EVENT:
				if(args != null && args.length == 4 && args[0] instanceof String && args[1] instanceof String && args[2] instanceof String && args[3] instanceof String) {
					String messageRaw = builder.getContent();
					String message = String.format(messageRaw, args[0], args[1], args[2], args[3]);
					target.sendMessage(bot, message);
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
					String messageRaw = builder.getContent();
					String message = String.format(messageRaw, args[0], args[1]);
					target.sendMessage(bot, message);
				}
				break;
			case NEW_PLAYER_JOIN:
			case PLAYER_JOIN:
			case PLAYER_LEAVE:
				if(args != null && args.length == 1 && args[0] instanceof String) {
					String messageRaw = builder.getContent();
					String message = String.format(messageRaw, args[0]);
					target.sendMessage(bot, message);
				}
				break;
			case SERVER_STARTING:
			case SERVER_STOPPING:
			case SERVER_RESTARTING:
			case LOG_INFO:
			case LOG_WARNING:
				if(args != null && args.length == 1 && args[0] instanceof String) {
					target.sendMessage(bot, builder.getContent() + " " + args[0]);
				}
				break;
			case SERVER_STOPPING_TIMED:
			case SERVER_RESTARTING_TIMED:
				if(args != null && args.length == 1 && args[0] instanceof Integer) {
					String messageRaw = builder.getContent();
					String message = String.format(messageRaw, args[0]);
					target.sendMessage(bot, message);
				}
				break;
			case SERVER_STARTED:
				if(args != null && args.length == 1 && args[0] instanceof Long) {
					String messageRaw = builder.getContent();
					String message = String.format(messageRaw, args[0]);
					target.sendMessage(bot, message);
				}
				break;
			case LOG_EXCEPTION:
			case LOG_FATAL:
				if(args != null && args[0] instanceof String) {
					if(args.length == 2) {
						if(args[1] instanceof Throwable) {
							Throwable exception = (Throwable) args[1];
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle((String) args[0]);
							embed.setDescription(exception.getMessage());
							embed.addField("Exception", StringTools.limit(Arrays.toString(exception.getStackTrace()), 1023), false);
							MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
							messageBuilder.setEmbeds(embed.build());
							target.sendMessage(bot, messageBuilder.build().getContent());
						} else {
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle((String) args[0]);
							embed.setDescription(args[1].toString());
							MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
							messageBuilder.setEmbeds(embed.build());
							target.sendMessage(bot, messageBuilder.build().getContent());
						}
					} else target.sendMessage(bot, builder.getContent() + " " + args[0]);
				}
				break;
		}
	}
}
