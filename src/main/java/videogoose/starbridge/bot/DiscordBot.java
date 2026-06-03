package videogoose.starbridge.bot;

import api.common.GameServer;
import api.listener.events.Event;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.config.FileConfiguration;
import api.utils.game.PlayerUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AccountManager;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.network.objects.ChatMessage;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.network.RegisteredClientOnServer;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.commands.CommandTypes;
import videogoose.starbridge.data.permissions.PermissionGroup;
import videogoose.starbridge.data.player.PlayerData;
import videogoose.starbridge.error.ErrorManager;
import videogoose.starbridge.manager.ConfigManager;
import videogoose.starbridge.server.ServerDatabase;
import videogoose.starbridge.utils.ExceptionStreamWatcher;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordBot extends ListenerAdapter implements Thread.UncaughtExceptionHandler {

	private final StarBridge instance;
	private final JDA bot;
	private final ConcurrentHashMap<Integer, PlayerData> linkRequestMap = new ConcurrentHashMap<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
		Thread thread = new Thread(runnable, "starbridge-scheduler");
		thread.setDaemon(true);
		return thread;
	});
	private long startTime;
	private boolean needsReset = true;

	private DiscordBot(StarBridge instance) {
		this.instance = instance;
		bot = createBot(ConfigManager.getMainConfig());
		installExceptionWatcher();
		try {
			assert bot != null;
			bot.awaitReady();
			startTime = System.currentTimeMillis();
			scheduler.scheduleAtFixedRate(this::updateChannelInfo, 60, 60, TimeUnit.SECONDS);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	/** Shuts down the background scheduler. */
	public void shutdown() {
		scheduler.shutdownNow();
	}

	public static DiscordBot initialize(StarBridge instance) {
		DiscordBot bot = new DiscordBot(instance);
		CommandTypes.registerDiscordCommands(bot);
		bot.initRestartTimer();
		return bot;
	}

	private void initRestartTimer() {
		long restartTimer = ConfigManager.getMainConfig().getLong("restart-timer");
		long shutdownTimer = ConfigManager.getMainConfig().getLong("default-shutdown-timer");
		assert restartTimer > shutdownTimer : "Restart timer must be greater than shutdown timer";
		if(restartTimer > 0) {
			if(shutdownTimer > 600000) {
				scheduler.schedule(() -> MessageType.SERVER_RESTARTING_TIMED.sendMessage(600), restartTimer - 600000, TimeUnit.MILLISECONDS);
				scheduler.schedule(() -> MessageType.SERVER_RESTARTING_TIMED.sendMessage(300), restartTimer - 300000, TimeUnit.MILLISECONDS);
				scheduler.schedule(() -> {
					MessageType.SERVER_RESTARTING_TIMED.sendMessage(60);
					GameServer.getServerState().addTimedShutdown(60);
				}, restartTimer - 60000, TimeUnit.MILLISECONDS);
			}
		}
	}

	/**
	 * Routes console exceptions printed by StarMade into the error registry.
	 */
	private void installExceptionWatcher() {
		System.setOut(new ExceptionStreamWatcher(System.out));
		System.setErr(new ExceptionStreamWatcher(System.err));
	}

	private JDA createBot(FileConfiguration config) {
		try {
			JDABuilder builder = JDABuilder.createDefault(config.getString("bot-token"), Arrays.asList(GatewayIntent.values()));
			builder.addEventListeners(this);
			return builder.build();
		} catch(Exception exception) {
			instance.logException("An exception occurred while creating Discord bot", exception);
			return null;
		}
	}

	public void updateChannelInfo() {
		try {
			int playerCount = GameServer.getServerState().getPlayerStatesByName().size();
			int playerMax = (int) ServerConfig.MAX_CLIENTS.getCurrentState();
			String chatChannelStats = ("Players: " + playerCount + " / " + playerMax);
			queueAction(Objects.requireNonNull(bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"))).getManager().setTopic(chatChannelStats));
			String logChannelStats = ("Clients: " + playerCount + " / " + playerMax + " \nCurrent Uptime: " + (System.currentTimeMillis() - startTime));
			queueAction(Objects.requireNonNull(bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id"))).getManager().setTopic(logChannelStats));
		} catch(Exception exception) {
			StarBridge.getInstance().logException("Failed to update channel info", exception);
		}
	}

	public Guild getGuild() {
		return bot.getGuildById(ConfigManager.getMainConfig().getLong("server-id"));
	}

	public boolean hasRole(Member member, long roleId) {
		for(Role role : member.getRoles()) {
			if(role.getIdLong() == roleId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String id = event.getComponentId();
		if(!id.startsWith(ErrorManager.BUTTON_PREFIX + ":")) return;
		if(!isStaff(event.getMember())) {
			event.reply("Only staff can manage error reports.").setEphemeral(true).queue();
			return;
		}
		String[] parts = id.split(":", 3);
		if(parts.length < 3) return;
		String action = parts[1];
		String fingerprint = parts[2];
		switch(action) {
			case "mute" -> {
				ErrorManager.muteFingerprint(fingerprint);
				event.reply("🔇 Muted error `" + fingerprint + "`. Future occurrences won't be posted.").setEphemeral(true).queue();
			}
			case "stack" -> {
				var entry = ErrorManager.getEntry(fingerprint);
				if(entry == null) event.reply("That error is no longer tracked.").setEphemeral(true).queue();
				else event.reply("```" + limit(entry.getFullText(), 1900) + "```").setEphemeral(true).queue();
			}
			case "pat" -> {
				var entry = ErrorManager.getEntry(fingerprint);
				String suggested = entry != null ? java.util.regex.Pattern.quote(entry.getExceptionClass()) : "";
				TextInput input = TextInput.create("regex", TextInputStyle.SHORT)
						.setValue(suggested)
						.setPlaceholder("e.g. java\\.io\\.FileNotFoundException")
						.setRequired(true)
						.build();
				Modal modal = Modal.create(ErrorManager.BUTTON_PREFIX + ":patmodal", "Mute error pattern")
						.addComponents(Label.of("Regex to mute", input))
						.build();
				event.replyModal(modal).queue();
			}
		}
	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if(!event.getModalId().equals(ErrorManager.BUTTON_PREFIX + ":patmodal")) return;
		if(!isStaff(event.getMember())) {
			event.reply("Only staff can manage error reports.").setEphemeral(true).queue();
			return;
		}
		var mapping = event.getValue("regex");
		String regex = mapping == null ? null : mapping.getAsString();
		boolean added = ErrorManager.mutePattern(regex);
		event.reply(added ? "🔇 Muted errors matching `" + regex + "`." : "Invalid or duplicate pattern.").setEphemeral(true).queue();
	}

	private boolean isStaff(Member member) {
		return member != null && hasRole(member, ConfigManager.getMainConfig().getLong("admin-role-id"));
	}

	private static String limit(String s, int max) {
		if(s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	/**
	 * Sends a message (typically an error report with action buttons) to the log channel.
	 */
	public void sendToLogChannel(MessageCreateData data) {
		try {
			var channel = bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id"));
			if(channel != null) channel.sendMessage(data).queue();
		} catch(Exception ignored) {
			// Never let a reporting failure cascade.
		}
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		boolean canUse = true;
		CommandTypes command = CommandTypes.getFromName(event.getName());
		if(event.getGuild() != null) {
			if(command != null) {
				if(command.permission.isAdminOnly()) {
					if(!hasRole(Objects.requireNonNull(event.getMember()), ConfigManager.getMainConfig().getLong("admin-role-id"))) {
						canUse = false;
					}
				} else {
					PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(event.getUser().getName());
					Object permission = playerData.getPermission(command.getPermission().left);
					if(permission != null) {
						if(!permission.equals(command.getPermission().right)) {
							canUse = false;
						}
					} else if(playerData.inAnyGroup()) {
						for(PermissionGroup group : playerData.getGroups()) {
							permission = group.getPermission(command.getPermission().left);
							if(permission != null) {
								if(!permission.equals(command.getPermission().right)) {
									canUse = false;
									break;
								}
							} else {
								canUse = false;
							}
						}
					}
				}
			}
		}
		if(!canUse) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
		} else if(command != null) {
			try {
				command.execute(event);
			} catch(Exception exception) {
				instance.logException("An exception occurred while executing command /" + event.getName(), exception);
				if(!event.isAcknowledged()) event.reply("An error occurred while trying to execute this command.").setEphemeral(true).queue();
			}
		} else {
			event.reply("An error occurred while trying to execute this command.").setEphemeral(true).queue();
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String content = event.getMessage().getContentDisplay().trim();
		if(!content.isEmpty()) {
			if(!event.getAuthor().isBot() && !event.isWebhookMessage()) {
				if(event.getChannel().getIdLong() == ConfigManager.getMainConfig().getLong("chat-channel-id")) {
					if(content.charAt(0) != '/') {
						sendServerMessage(event.getAuthor().getEffectiveName(), content.trim());
					} else {
						event.getMessage().delete().queue();
					}
				}
			}
		}
	}

	/**
	 * Sends a message to the in-game chat.
	 *
	 * @param message the message to send.
	 */
	public void sendServerMessage(String sender, String message) {
		try {
			for(RegisteredClientOnServer client : GameServerState.instance.getClients().values()) {
				if(sender != null) {
					client.serverMessage("[" + sender + "] " + message);
				} else {
					client.serverMessage(message);
				}
			}
		} catch(Exception exception) {
			instance.logException("An exception occurred while sending server message", exception);
		}
	}

	public void addLinkRequest(PlayerState playerState) {
		PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerState.getName());
		removeLinkRequest(playerData);
		int num = (new Random()).nextInt(9999 - 1000) + 1000;
		linkRequestMap.put(num, playerData);
		PlayerUtils.sendMessage(playerState, "Use /link " + num + " in #" + bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id")).getName() + " to link your account. This code will expire in 15 minutes.");
		scheduler.schedule(() -> removeLinkRequest(playerData), 15, TimeUnit.MINUTES);
	}

	public void removeLinkRequest(PlayerData playerData) {
		for(Integer key : linkRequestMap.keySet()) {
			if(linkRequestMap.get(key).equals(playerData)) {
				linkRequestMap.remove(key);
				return;
			}
		}
	}

	public void handleEvent(Event event) {
		if(!event.isServer()) return;
        switch (event) {
            case PlayerCustomCommandEvent playerCustomCommandEvent -> {
                if (playerCustomCommandEvent.getCommand().isAdminOnly() && !playerCustomCommandEvent.getSender().isAdmin()) {
                    return;
                }
                StarBridge.getInstance().logInfo(playerCustomCommandEvent.getSender().getName() + " executed command: " + playerCustomCommandEvent.getCommand().getCommand());
            }
            case PlayerChatEvent playerChatEvent -> {
                String message = playerChatEvent.getMessage().text;
                message = message.replace("@", "");
                message = message.replace("\"", "");
                ChatMessage chatMessage = new ChatMessage(playerChatEvent.getMessage());
                PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerChatEvent.getMessage().sender);
                try {
                    switch (chatMessage.receiverType) {
                        case SYSTEM:
                            StarBridge.getInstance().logInfo(chatMessage.sender + ": " + message);
                            break;
                        case DIRECT:
                            if (chatMessage.receiver != null) {
                                String messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
                                StarBridge.getInstance().logInfo(messageToSend);
                            }
                            break;
                        case CHANNEL:
                            if (chatMessage.getChannel() != null) {
                                String messageToSend;
                                try {
                                    switch (chatMessage.getChannel().getType()) {
                                        case FACTION:
                                            int factionId = Integer.parseInt(chatMessage.receiver.substring(chatMessage.receiver.indexOf("Faction") + 7));
                                            String factionName = GameServer.getServerState().getFactionManager().getFaction(factionId).getName();
                                            messageToSend = chatMessage.sender + " -> [" + factionName + "]: " + message;
                                            break;
                                        case PUBLIC:
                                            messageToSend = chatMessage.sender + " -> Public: " + message;
//										long discordID = playerData.getDiscordId();
                                            try {
//											if(discordID > 0) setBotToUser(Objects.requireNonNull(bot.getUserById(discordID)));
//											else queueAction(bot.getSelfUser().getManager().setName(chatMessage.sender));
                                                sendDiscordMessage(new MessageCreateBuilder().addContent("[" + chatMessage.sender + "]: " + message).build());
//											resetBot();
                                            } catch (Exception exception) {
                                                instance.logException("An exception occurred while trying to handle a chat event", exception);
                                            }
                                            break;
                                        case PARTY:
                                            messageToSend = chatMessage.sender + " -> Party: " + message;
                                            break;
                                        default:
                                            messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
                                            break;
                                    }
                                } catch (Exception ignored) {
                                    messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
                                }
                                StarBridge.getInstance().logInfo(messageToSend);
                            } else if (chatMessage.receiver.toLowerCase(Locale.ENGLISH).startsWith("faction")) {
                                try {
                                    int factionId = Integer.parseInt(chatMessage.receiver.substring(chatMessage.receiver.indexOf("Faction") + 7));
                                    String factionName = GameServer.getServerState().getFactionManager().getFaction(factionId).getName();
                                    StarBridge.getInstance().logInfo(chatMessage.sender + " -> [" + factionName + "]: " + message);
                                } catch (Exception ignored) {
                                    StarBridge.getInstance().logInfo(chatMessage.sender + " -> [Unknown Faction]: " + message);
                                }
                            } else if ("all".equalsIgnoreCase(chatMessage.receiver)) {
//							long discordID = playerData.getDiscordId();
//							if(discordID > 0) setBotToUser(Objects.requireNonNull(bot.getUserById(discordID)));
//							queueAction(bot.getSelfUser().getManager().setName(chatMessage.sender));
                                sendDiscordMessage(new MessageCreateBuilder().addContent("[" + chatMessage.sender + "]: " + message).build());
//							resetBot();
                            }
                            break;
                    }
                } catch (Exception exception) {
                    StarBridge.getInstance().logException("An exception occurred while trying to handle a chat event", exception);
                }
            }
            case PlayerJoinWorldEvent playerJoinWorldEvent -> {
                PlayerData playerData = ServerDatabase.getPlayerData(playerJoinWorldEvent.getPlayerName());
                if (playerData == null) {
                    ServerDatabase.addNewPlayerData(playerJoinWorldEvent.getPlayerName());
                    MessageType.NEW_PLAYER_JOIN.sendMessage(playerJoinWorldEvent.getPlayerName());
                } else MessageType.PLAYER_JOIN.sendMessage(playerJoinWorldEvent.getPlayerName());
            }
            case PlayerLeaveWorldEvent playerLeaveWorldEvent ->
                    MessageType.PLAYER_LEAVE.sendMessage(playerLeaveWorldEvent.getPlayerName());
            case FactionCreateEvent factionCreateEvent ->
                    MessageType.FACTION_CREATE.sendMessage(factionCreateEvent.getPlayer().getName(), factionCreateEvent.getFaction().getName());
            case PlayerJoinFactionEvent playerJoinFactionEvent ->
                    MessageType.PLAYER_JOIN_FACTION.sendMessage(playerJoinFactionEvent.getPlayer(), playerJoinFactionEvent.getPlayer().getFactionName());
            case PlayerLeaveFactionEvent playerLeaveFactionEvent ->
                    MessageType.PLAYER_LEAVE_FACTION.sendMessage(playerLeaveFactionEvent.getPlayer(), playerLeaveFactionEvent.getFaction().getName());
            case PlayerDeathEvent playerDeathEvent -> {
                if (playerDeathEvent.getDamager().isSegmentController()) {
                    SegmentController segmentController = (SegmentController) playerDeathEvent.getDamager();
                    String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
                    String entityFactionName = (segmentController.getFactionId() != 0) ? segmentController.getFaction().getName() : "No Faction";
                    MessageType.PLAYER_KILL_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), playerFactionName, segmentController.getRealName(), entityFactionName);
                } else if (playerDeathEvent.getDamager() instanceof PlayerState killerState) {
                    String killerFactionName = (killerState.getFactionId() != 0) ? killerState.getFactionName() : "No Faction";
                    String killedFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
                    if (killerState.equals(playerDeathEvent.getPlayer()))
                        MessageType.PLAYER_SUICIDE_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), killedFactionName);
                    else
                        MessageType.PLAYER_KILL_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), killedFactionName, killerState.getName(), killerFactionName);
                } else {
                    String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
                    MessageType.PLAYER_DEATH_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), playerFactionName);
                }
            }
            case FactionRelationChangeEvent factionRelationChangeEvent -> {
                if (factionRelationChangeEvent.getFrom().getIdFaction() <= 0 || factionRelationChangeEvent.getTo().getIdFaction() <= 0)
                    return;
                FactionRelation.RType oldRelation = factionRelationChangeEvent.getOldRelation();
                FactionRelation.RType newRelation = factionRelationChangeEvent.getNewRelation();
                switch (oldRelation) {
                    case NEUTRAL:
                        switch (newRelation) {
                            case FRIEND:
                                MessageType.FACTION_ALLY.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                break;
                            case ENEMY:
                                MessageType.FACTION_DECLARE_WAR.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                break;
                        }
                        break;
                    case ENEMY:
                        switch (newRelation) {
                            case NEUTRAL:
                                MessageType.FACTION_PEACE.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                break;
                            case FRIEND:
                                MessageType.FACTION_PEACE.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                MessageType.FACTION_ALLY.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                break;
                        }
                        break;
                    case FRIEND:
                        switch (newRelation) {
                            case NEUTRAL:
                                MessageType.FACTION_CANCEL_ALLY.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                break;
                            case ENEMY:
                                MessageType.FACTION_CANCEL_ALLY.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                MessageType.FACTION_DECLARE_WAR.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
                                break;
                        }
                        break;
                }
            }
            default -> {
            }
        }
	}

	public void setBotToUser(User user) {
		try {
			AccountManager manager = bot.getSelfUser().getManager();
//			Icon icon = getAvatar(user.getEffectiveAvatarUrl());
//			if(icon != null) {
//				queueAction(manager.setName(user.getEffectiveName()));
//				queueAction(manager.setAvatar(icon));
			needsReset = true;
//			}
		} catch(Exception exception) {
			instance.logException("An exception occurred while setting bot to user", exception);
		}
	}

	private void queueAction(RestAction<?> action) {
		action.queue();
	}

	public void resetBot() {
		try {
			AccountManager manager = bot.getSelfUser().getManager();
//			queueAction(manager.setName(ConfigManager.getMainConfig().getString("bot-name")));
//			queueAction(manager.setAvatar(getAvatar(ConfigManager.getMainConfig().getString("bot-avatar"))));
			needsReset = false;
		} catch(Exception exception) {
			instance.logException("An exception occurred while resetting bot", exception);
		}
	}

	public String getURL(String string) {
		if(string.startsWith("\"") && string.endsWith("\"")) string = string.substring(1, string.length() - 1);
		if(!string.startsWith("https://")) string = "https://" + string;
		return string;
	}

	private Icon getAvatar(String avatarUrl) {
		try {
			avatarUrl = getURL(avatarUrl);
			URLConnection connection = new URL(avatarUrl).openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			InputStream inputStream = connection.getInputStream();
			return Icon.from(inputStream);
		} catch(Exception exception) {
			instance.logException("An exception occurred while setting bot avatar", exception);
			return null;
		}
	}

	/**
	 * Sends a message to the Discord channel.
	 *
	 * @param message the message to send.
	 */
	public void sendDiscordMessage(MessageCreateData message) {
		try {
			queueAction(Objects.requireNonNull(bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"))).sendMessage(message));
		} catch(Exception exception) {
			instance.logException("An exception occurred while sending Discord message", exception);
		}
	}

	public JDA getJDA() {
		return bot;
	}

	public PlayerData getLinkRequest(int linkCode) {
		return linkRequestMap.get(linkCode);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		ErrorManager.reportFatal("Uncaught exception in thread " + thread.getName(), exception);
	}

	public void checkReset() {
		if(needsReset) resetBot();
	}
}
