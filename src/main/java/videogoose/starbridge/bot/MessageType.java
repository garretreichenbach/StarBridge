package videogoose.starbridge.bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.schema.common.util.StringTools;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.manager.ConfigManager;
import videogoose.starbridge.utils.DiscordUtils;

import java.util.Arrays;

/**
 * Enum used for defining different types of server messages from the bot.
 *
 * <p>Should not be used for user messages, just for things like server status updates.
 *
 * <p><b>Echo-guard contract:</b> Every case that calls
 * {@link DiscordBot#sendBridgeMessage(String)} registers the injected text with
 * the echo guard so the bridge won't re-relay it when it echoes back from
 * Discord or appears in a {@link api.listener.events.player.PlayerChatEvent}.
 *
 * <p><b>sendServerMessage vs sendBridgeMessage:</b>
 * <ul>
 *   <li>Server-event messages (kills, join/leave, faction events) that were
 *       historically also broadcast in-game via
 *       {@code sendServerMessage("Server", …)} caused the feedback loop.  Those
 *       broadcasts ARE intentional — players should see them in chat — so they
 *       are now routed through {@link DiscordBot#sendBridgeMessage(String)}
 *       which both injects the text AND registers it in the echo guard.</li>
 *   <li>Pure-log cases (LOG_*, SERVER_STARTING, SERVER_STOPPING, …) never
 *       called sendServerMessage in the original code and still do not.</li>
 * </ul>
 *
 * @author VideoGoose
 */
public enum MessageType {
	PLAYER_KILL_EVENT(new MessageCreateBuilder().addContent(":skull_crossbones: %s [%s] was killed by %s").build(), ChannelTarget.BOTH),
	PLAYER_DEATH_EVENT(new MessageCreateBuilder().addContent(":skull: %s [%s] has died").build(), ChannelTarget.BOTH),
	PLAYER_SUICIDE_EVENT(new MessageCreateBuilder().addContent(":skull: %s [%s] has taken their own life").build(), ChannelTarget.BOTH),
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
	SERVER_STARTED(new MessageCreateBuilder().addContent(":white_check_mark: Server started in %sms").build(), ChannelTarget.BOTH),
	SERVER_STOPPING(new MessageCreateBuilder().addContent(":stop_sign: Server is stopping...").build(), ChannelTarget.BOTH),
	SERVER_STOPPING_TIMED(new MessageCreateBuilder().addContent(":stop_sign: Server will stop in %s").build(), ChannelTarget.BOTH),
	SERVER_RESTARTING_TIMED(new MessageCreateBuilder().addContent(":arrows_counterclockwise: Server will restart in %s").build(), ChannelTarget.BOTH),
	SERVER_RESTARTING(new MessageCreateBuilder().addContent(":arrows_counterclockwise: Server is restarting...").build(), ChannelTarget.BOTH),
	SERVER_CRASHED(new MessageCreateBuilder().addContent(":x: Server has crashed due to a(n) %s! Attempting restart...").build(), ChannelTarget.BOTH),
	DEBUG_MODE_STARTED(new MessageCreateBuilder().addContent(":question: [DEBUG] Server started in debug mode").build(), ChannelTarget.BOTH),
	LOG_DEBUG(new MessageCreateBuilder().addContent(":question: [DEBUG] %s").build(), ChannelTarget.LOG),
	LOG_INFO(new MessageCreateBuilder().addContent(":information_source: [INFO] %s").build(), ChannelTarget.LOG),
	LOG_WARNING(new MessageCreateBuilder().addContent(":warning: [WARNING] %s").build(), ChannelTarget.LOG),
	LOG_EXCEPTION(new MessageCreateBuilder().addContent(":exclamation: [EXCEPTION] %s").build(), ChannelTarget.LOG),
	LOG_FATAL(new MessageCreateBuilder().addContent(":bangbang: [FATAL] %s").build(), ChannelTarget.LOG),
	CHANGELOG(new MessageCreateBuilder().addContent(":scroll: %s").build(), ChannelTarget.CHANGELOG);

	private final MessageCreateData builder;
	private final ChannelTarget target;

	MessageType(MessageCreateData builder, ChannelTarget target) {
		this.builder = builder;
		this.target = target;
	}

	public void sendMessage(Object... args) {
		JDA bot = StarBridge.getBot().getJDA();
		switch (this) {
			case PLAYER_KILL_EVENT -> {
				// 4-arg variant: killed, killedFaction, killer, killerFaction
				if (args != null && args.length == 4
						&& args[0] instanceof String killed
						&& args[1] instanceof String killedFaction
						&& args[2] instanceof String killer
						&& args[3] instanceof String killerFaction) {
					var message = builder.getContent().formatted(killed, killedFaction, killer, killerFaction);
					target.sendMessage(bot, message);
					// Intentional in-game announcement; routed through sendBridgeMessage so
					// the echo guard prevents the relay from bouncing back.
					StarBridge.getBot().sendBridgeMessage(message);
				}
			}
			case FACTION_CREATE, FACTION_DECLARE_WAR, FACTION_PEACE, FACTION_ALLY,
					FACTION_CANCEL_ALLY, PLAYER_JOIN_FACTION, PLAYER_LEAVE_FACTION,
					PLAYER_DEATH_EVENT, PLAYER_SUICIDE_EVENT -> {
				// 2-arg variant: entity1, entity2
				if (args != null && args.length == 2
						&& args[0] instanceof String a0
						&& args[1] instanceof String a1) {
					var message = builder.getContent().formatted(a0, a1);
					target.sendMessage(bot, message);
					// Intentional in-game announcement; registered with echo guard.
					StarBridge.getBot().sendBridgeMessage(message);
				}
			}
			case NEW_PLAYER_JOIN, PLAYER_JOIN, PLAYER_LEAVE -> {
				// 1-arg variant: player name
				if (args != null && args.length == 1 && args[0] instanceof String a0) {
					var message = builder.getContent().formatted(a0);
					target.sendMessage(bot, message);
					// Intentional in-game announcement; registered with echo guard.
					StarBridge.getBot().sendBridgeMessage(message);
				}
			}
			case SERVER_STARTING, SERVER_STOPPING, SERVER_RESTARTING, LOG_INFO, LOG_WARNING -> {
				// 1-arg (or 0-arg for no-placeholder variants)
				if (args != null && args.length == 1 && args[0] instanceof String a0) {
					target.sendMessage(bot, builder.getContent() + " " + a0);
					// Pure log/status — does NOT broadcast in-game (no change from original).
				} else if ((args == null || args.length == 0) && !builder.getContent().contains("%s")) {
					target.sendMessage(bot, builder.getContent());
				}
			}
			case LOG_DEBUG -> {
				if (ConfigManager.getMainConfig().getBoolean("debug-mode")
						&& args != null && args.length == 1 && args[0] instanceof String a0) {
					target.sendMessage(bot, builder.getContent() + " " + a0);
				}
			}
			case SERVER_STOPPING_TIMED, SERVER_RESTARTING_TIMED -> {
				if (args != null && args.length == 1) {
					int seconds = switch (args[0]) {
						case Long l -> (int) (l / 1000);
						case Integer i -> i;
						default -> -1;
					};
					if (seconds >= 60) {
						var message = builder.getContent().formatted(seconds / 60 + " minutes");
						target.sendMessage(bot, message);
						// Intentional timed-shutdown announcement; registered with echo guard.
						StarBridge.getBot().sendBridgeMessage(message);
					} else if (seconds >= 0) {
						var message = builder.getContent().formatted(seconds + " seconds");
						target.sendMessage(bot, message);
						StarBridge.getBot().sendBridgeMessage(message);
					}
				}
			}
			case SERVER_STARTED -> {
				if (args != null && args.length == 1 && args[0] instanceof Long l) {
					var message = builder.getContent().formatted(l);
					target.sendMessage(bot, message);
					// SERVER_STARTED is a one-time boot notification; only sent to Discord/log,
					// not broadcast in-game (no sendBridgeMessage needed here).
				}
			}
			case DEBUG_MODE_STARTED, SERVER_CRASHED -> {
				// Zero-arg or 1-arg fallback.
				// DEBUG_MODE_STARTED has no %s placeholder; SERVER_CRASHED has one.
				if (args == null || args.length == 0) {
					target.sendMessage(bot, builder.getContent());
				} else if (args.length == 1 && args[0] instanceof String a0) {
					var message = builder.getContent().formatted(a0);
					target.sendMessage(bot, message);
				}
			}
			case LOG_EXCEPTION -> {
				if (args != null && args[0] instanceof String title) {
					target.sendMessage(bot, buildExceptionMessage(title, args.length >= 2 ? args[1] : null));
				}
			}
			case LOG_FATAL -> {
				if (args != null && args[0] instanceof String title) {
					target.sendMessage(bot, buildFatalMessage(title, args.length >= 2 ? args[1] : null));
				}
			}
			case CHANGELOG -> {
				// 1-arg variant: pre-formatted changelog body (may be multi-line).
				if (args != null && args.length == 1 && args[0] instanceof String a0) {
					target.sendMessage(bot, builder.getContent().formatted(a0));
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private static String buildExceptionMessage(String title, Object secondArg) {
		var messageBuilder = new MessageCreateBuilder();
		if (secondArg instanceof Exception exception) {
			var embed = new EmbedBuilder()
					.setTitle(title)
					.setDescription(exception.getClass().getSimpleName())
					.addField("Stack Trace", StringTools.limit(Arrays.toString(exception.getStackTrace()), 1024), false)
					.build();
			messageBuilder.setEmbeds(embed);
		} else if (secondArg != null) {
			var embed = new EmbedBuilder()
					.setTitle(title)
					.setDescription(secondArg.getClass().getSimpleName())
					.build();
			messageBuilder.setEmbeds(embed);
		} else {
			messageBuilder.setContent(StringTools.limit("Server encountered an exception\n:" + title, 2000));
		}
		return StringTools.limit(messageBuilder.build().getContent(), 2000);
	}

	private static String buildFatalMessage(String title, Object secondArg) {
		var messageBuilder = new MessageCreateBuilder();
		Role staffRole = DiscordUtils.getStaffRole();
		String mention = (staffRole != null) ? staffRole.getAsMention() + " " : "";

		if (secondArg instanceof Exception exception) {
			var embed = new EmbedBuilder()
					.setTitle(title)
					.setDescription(exception.getClass().getSimpleName())
					.addField("Stack Trace", StringTools.limit(Arrays.toString(exception.getStackTrace()), 1023), false)
					.build();
			messageBuilder.setContent(mention + "Server crashed due to a severe exception\n:" + exception.getMessage());
			messageBuilder.setEmbeds(embed);
		} else if (secondArg != null) {
			var embed = new EmbedBuilder()
					.setTitle(title)
					.setDescription(secondArg.getClass().getSimpleName())
					.build();
			messageBuilder.setContent(mention + "Server crashed due to a severe exception\n:" + secondArg);
			messageBuilder.setEmbeds(embed);
		} else {
			messageBuilder.setContent(mention + "Server crashed due to a severe exception\n:" + title);
		}
		return messageBuilder.build().getContent();
	}
}
