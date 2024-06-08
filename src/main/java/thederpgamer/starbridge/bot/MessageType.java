package thederpgamer.starbridge.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.schema.common.util.StringTools;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.utils.DiscordUtils;

import java.util.Arrays;

/**
 * Enum used for defining different types of server messages from the bot.
 * <br/>Should not be used for user messages, just for things like server status updates.
 *
 * @author Garret Reichenbach
 */
public enum MessageType {
	PLAYER_KILL_EVENT(new MessageCreateBuilder().addContent(":skull_crossbones: %s [%s] was killed by %s").build(), ChannelTarget.BOTH),
	PLAYER_DEATH_EVENT(new MessageCreateBuilder().addContent(":skull: %s [%s] has died").build(), ChannelTarget.BOTH),
	PLAYER_SUICIDE_EVENT(new MessageCreateBuilder().addContent(":skull: s [%s] has taken their own life").build(), ChannelTarget.BOTH),
	FACTION_CREATE(new MessageCreateBuilder().addContent(":shield: %s created a new faction %s").build(), ChannelTarget.BOTH),
	FACTION_DECLARE_WAR(new MessageCreateBuilder().addContent(":crossed_swords: %s has declared war on %s").build(), ChannelTarget.BOTH),
	FACTION_PEACE(new MessageCreateBuilder().addContent(":dove: %s has agreed to a peace deal with %s").build(), ChannelTarget.BOTH),
	FACTION_ALLY(new MessageCreateBuilder().addContent(":shield: %s has agreed to an alliance with %s").build(), ChannelTarget.BOTH),
	FACTION_CANCEL_ALLY(new MessageCreateBuilder().addContent(":x: %s has broken off from their alliance with %s").build(), ChannelTarget.BOTH),
	PLAYER_JOIN_FACTION(new MessageCreateBuilder().addContent(":heavy_plus_sign: %s joined the faction %s").build(), ChannelTarget.BOTH),
	PLAYER_LEAVE_FACTION(new MessageCreateBuilder().addContent(":heavy_minus_sign: %s left the faction %s").build(), ChannelTarget.BOTH),
	NEW_PLAYER_JOIN(new MessageCreateBuilder().addContent(":tada: %s joined the server for the first time").build(), ChannelTarget.BOTH),
	PLAYER_JOIN(new MessageCreateBuilder().addContent(":inbox_tray: %s joined the server").build(), ChannelTarget.BOTH),
	PLAYER_LEAVE(new MessageCreateBuilder().addContent(":outbox_tray: %s left the server").build(), ChannelTarget.BOTH),
	SERVER_STARTING(new MessageCreateBuilder().addContent(":white_check_mark: Server is starting...").build(), ChannelTarget.BOTH),
	SERVER_STARTED(new MessageCreateBuilder().addContent(":white_check_mark: Server started in %dms").build(), ChannelTarget.BOTH),
	SERVER_STOPPING(new MessageCreateBuilder().addContent(":stop_sign: Server is stopping...").build(), ChannelTarget.BOTH),
	SERVER_STOPPING_TIMED(new MessageCreateBuilder().addContent(":stop_sign: Server will stop in %d").build(), ChannelTarget.BOTH),
	SERVER_RESTARTING_TIMED(new MessageCreateBuilder().addContent(":arrows_counterclockwise: Server will restart in %d").build(), ChannelTarget.BOTH),
	SERVER_RESTARTING(new MessageCreateBuilder().addContent(":arrows_counterclockwise: Server is restarting...").build(), ChannelTarget.BOTH),
	LOG_INFO(new MessageCreateBuilder().addContent(":information_source [INFO]:").build(), ChannelTarget.LOG),
	LOG_WARNING(new MessageCreateBuilder().addContent(":warning: [WARNING]:").build(), ChannelTarget.LOG),
	LOG_EXCEPTION(new MessageCreateBuilder().addContent(":exclamation: [EXCEPTION]:").build(), ChannelTarget.LOG),
	LOG_FATAL(new MessageCreateBuilder().addContent(":bangbang: [FATAL]:").build(), ChannelTarget.LOG);

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
				if(args != null && args.length == 1) {
					int seconds = -1;
					if(args[0] instanceof Long) seconds = (int) ((long) args[0] / 1000);
					else if(args[0] instanceof Integer) seconds = (int) args[0];
					if(seconds >= 60) {
						int minutes = seconds / 60;
						String messageRaw = builder.getContent();
						String message = String.format(messageRaw, minutes + " minutes");
						target.sendMessage(bot, message);
					} else {
						String messageRaw = builder.getContent();
						String message = String.format(messageRaw, seconds + " seconds");
						target.sendMessage(bot, message);
					}
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
				if(args != null && args[0] instanceof String) {
					if(args.length == 2) {
						if(args[1] instanceof Throwable) {
							Throwable exception = (Throwable) args[1];
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle((String) args[0]);
							embed.setDescription(exception.getClass().getSimpleName());
							embed.addField("Stack Trace", StringTools.limit(Arrays.toString(exception.getStackTrace()), 1023), false);
							MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
							messageBuilder.setContent("Server encountered an exception\n:" + exception.getMessage());
							messageBuilder.setEmbeds(embed.build());
							target.sendMessage(bot, messageBuilder.build().getContent());
						} else {
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle((String) args[0]);
							embed.setDescription(args[1].getClass().getSimpleName());
							MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
							messageBuilder.setContent("Server encountered an exception\n:" + args[1].toString());
							messageBuilder.setEmbeds(embed.build());
							target.sendMessage(bot, messageBuilder.build().getContent());
						}
					} else {
						MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
						messageBuilder.setContent("Server encountered an exception\n:" + args[0]);
						target.sendMessage(bot, messageBuilder.build().getContent());
					}
				}
				break;
			case LOG_FATAL:
				if(args != null && args[0] instanceof String) {
					if(args.length == 2) {
						if(args[1] instanceof Throwable) {
							Throwable exception = (Throwable) args[1];
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle((String) args[0]);
							embed.setDescription(exception.getClass().getSimpleName());
							embed.addField("Stack Trace", StringTools.limit(Arrays.toString(exception.getStackTrace()), 1023), false);
							MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
							Role staffRole = DiscordUtils.getStaffRole();
							if(staffRole != null) messageBuilder.setContent(staffRole.getAsMention() + " Server crashed due to a severe exception\n:" + exception.getMessage());
							else messageBuilder.setContent("Server crashed due to a severe exception\n:" + exception.getMessage());
							messageBuilder.setEmbeds(embed.build());
							target.sendMessage(bot, messageBuilder.build().getContent());
						} else {
							EmbedBuilder embed = new EmbedBuilder();
							embed.setTitle((String) args[0]);
							embed.setDescription(args[1].getClass().getSimpleName());
							MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
							Role staffRole = DiscordUtils.getStaffRole();
							if(staffRole != null) messageBuilder.setContent(staffRole.getAsMention() + " Server crashed due to a severe exception\n:" + args[1].toString());
							else messageBuilder.setContent("Server crashed due to a severe exception\n:" + args[1].toString());
							messageBuilder.setEmbeds(embed.build());
							target.sendMessage(bot, messageBuilder.build().getContent());
						}
					} else {
						MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
						Role staffRole = DiscordUtils.getStaffRole();
						if(staffRole != null) messageBuilder.setContent(staffRole.getAsMention() + " Server crashed due to a severe exception\n:" + args[0]);
						else messageBuilder.setContent("Server crashed due to a severe exception\n:" + args[0]);
						target.sendMessage(bot, messageBuilder.build().getContent());
					}
				}
				System.exit(1);
				break;
		}
	}
}
