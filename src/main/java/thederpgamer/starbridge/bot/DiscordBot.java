package thederpgamer.starbridge.bot;

import api.common.GameCommon;
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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AccountManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.network.objects.ChatMessage;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.network.RegisteredClientOnServer;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.commands.CommandTypes;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.ui.DiscordUI;
import thederpgamer.starbridge.utils.LogWatcher;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class DiscordBot extends ListenerAdapter implements Thread.UncaughtExceptionHandler {

	private final StarBridge instance;
	private final JDA bot;
	private final HashMap<String, DiscordUI> uiMap = new HashMap<>();
	private final ConcurrentHashMap<Integer, PlayerData> linkRequestMap = new ConcurrentHashMap<>();
	private long startTime;
	private boolean needsReset = true;

	private DiscordBot(StarBridge instance) {
		this.instance = instance;
		bot = createBot(ConfigManager.getMainConfig());
		initLogWatcher();
		try {
			assert bot != null;
			bot.awaitReady();
			startTime = System.currentTimeMillis();

			new Thread(() -> {
				while(true) {
					try {
						Thread.sleep(60000);
						updateChannelInfo();
					} catch(Exception exception) {
						exception.printStackTrace();
					}
				}
			}).start();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
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
				long minuteWarning10 = restartTimer - 600000; //10 minute warning
				long minuteWarning5 = restartTimer - 300000; //5 minute warning
				long minuteWarning1 = restartTimer - 60000; //1 minute warning
				Timer restart = new Timer("restart-timer");
				restart.schedule(new TimerTask() {
					@Override
					public void run() {
						MessageType.SERVER_RESTARTING_TIMED.sendMessage(600);
					}
				}, minuteWarning10);
				restart.schedule(new TimerTask() {
					@Override
					public void run() {
						MessageType.SERVER_RESTARTING_TIMED.sendMessage(300);
					}
				}, minuteWarning5);
				restart.schedule(new TimerTask() {
					@Override
					public void run() {
						MessageType.SERVER_RESTARTING_TIMED.sendMessage(60);
						GameServer.getServerState().addTimedShutdown(60);
					}
				}, minuteWarning1);
			}
		}
	}

	/**
	 * Adds a watcher to output streams to log exceptions.
	 */
	private void initLogWatcher() {
		System.setOut(new LogWatcher(System.out));
		System.setErr(new LogWatcher(System.err));
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
		for(Role role : member.getRoles()) if(role.getIdLong() == roleId) return true;
		return false;
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		if(event.getGuild() != null) {
			for(DiscordUI ui : uiMap.values()) {
				if(ui.hasComponent(event.getComponentId())) {
					ui.handleInteraction(event.getComponentId(), event);
					return;
				}
			}
		}
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if(event.getGuild() != null) {
			CommandTypes command = CommandTypes.getFromName(event.getName());
			if(command != null) {
				//Todo: Handle permissions check
			}
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String content = event.getMessage().getContentDisplay().trim();
		if(!content.isEmpty()) {
			if(!event.getAuthor().isBot() && !event.isWebhookMessage()) {
				if(event.getChannel().getIdLong() == ConfigManager.getMainConfig().getLong("chat-channel-id")) {
					if(content.charAt(0) != '/') sendServerMessage(event.getAuthor().getEffectiveName(), content.trim());
					else event.getMessage().delete().queue();
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
				if(sender != null) client.serverMessage("[" + sender + "] " + message);
				else client.serverMessage(message);
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
		new Timer("link_timer_" + linkRequestMap.get(num).getPlayerName()).schedule(new TimerTask() {
			@Override
			public void run() {
				removeLinkRequest(playerData);
			}
		}, 900000);
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
		if(event instanceof PlayerCustomCommandEvent) {
			PlayerCustomCommandEvent playerCustomCommandEvent = (PlayerCustomCommandEvent) event;
			if(playerCustomCommandEvent.getCommand().isAdminOnly() && !playerCustomCommandEvent.getSender().isAdmin()) return;
			StarBridge.getInstance().logInfo(playerCustomCommandEvent.getSender().getName() + " executed command: " + playerCustomCommandEvent.getCommand().getCommand());
		} else if(event instanceof PlayerChatEvent) {
			PlayerChatEvent playerChatEvent = (PlayerChatEvent) event;
			String message = playerChatEvent.getMessage().text;
			message = message.replace("@", "");
			message = message.replace("\"", "");
			ChatMessage chatMessage = new ChatMessage(playerChatEvent.getMessage());
			PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerChatEvent.getMessage().sender);
			try {
				switch(chatMessage.receiverType) {
					case SYSTEM:
						StarBridge.getInstance().logInfo(chatMessage.sender + ": " + message);
						break;
					case DIRECT:
						if(chatMessage.receiver != null) {
							String messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
							StarBridge.getInstance().logInfo(messageToSend);
						}
						break;
					case CHANNEL:
						if(chatMessage.getChannel() != null) {
							String messageToSend;
							try {
								switch(chatMessage.getChannel().getType()) {
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
										} catch(Exception exception) {
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
							} catch(Exception ignored) {
								messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
							}
							StarBridge.getInstance().logInfo(messageToSend);
						} else if(chatMessage.receiver.toLowerCase(Locale.ENGLISH).startsWith("faction")) {
							try {
								int factionId = Integer.parseInt(chatMessage.receiver.substring(chatMessage.receiver.indexOf("Faction") + 7));
								String factionName = GameServer.getServerState().getFactionManager().getFaction(factionId).getName();
								StarBridge.getInstance().logInfo(chatMessage.sender + " -> [" + factionName + "]: " + message);
							} catch(Exception ignored) {
								StarBridge.getInstance().logInfo(chatMessage.sender + " -> [Unknown Faction]: " + message);
							}
						} else if("all".equalsIgnoreCase(chatMessage.receiver)) {
//							long discordID = playerData.getDiscordId();
//							if(discordID > 0) setBotToUser(Objects.requireNonNull(bot.getUserById(discordID)));
//							queueAction(bot.getSelfUser().getManager().setName(chatMessage.sender));
							sendDiscordMessage(new MessageCreateBuilder().addContent("[" + chatMessage.sender + "]: " + message).build());
//							resetBot();
						}
						break;
				}
			} catch(Exception exception) {
				StarBridge.getInstance().logException("An exception occurred while trying to handle a chat event", exception);
			}
		} else if(event instanceof PlayerJoinWorldEvent) {
			PlayerJoinWorldEvent playerJoinWorldEvent = (PlayerJoinWorldEvent) event;
			PlayerData playerData = ServerDatabase.getPlayerData(((PlayerJoinWorldEvent) event).getPlayerName());
			if(playerData == null) {
				ServerDatabase.addNewPlayerData(playerJoinWorldEvent.getPlayerName());
				MessageType.NEW_PLAYER_JOIN.sendMessage(playerJoinWorldEvent.getPlayerName());
			} else MessageType.PLAYER_JOIN.sendMessage(playerJoinWorldEvent.getPlayerName());
		} else if(event instanceof PlayerLeaveWorldEvent) {
			PlayerLeaveWorldEvent playerLeaveWorldEvent = (PlayerLeaveWorldEvent) event;
			MessageType.PLAYER_LEAVE.sendMessage(playerLeaveWorldEvent.getPlayerName());
		} else if(event instanceof FactionCreateEvent) {
			FactionCreateEvent factionCreateEvent = (FactionCreateEvent) event;
			MessageType.FACTION_CREATE.sendMessage(factionCreateEvent.getFaction().getName(), factionCreateEvent.getPlayer().getName());
		} else if(event instanceof PlayerJoinFactionEvent) {
			PlayerJoinFactionEvent playerJoinFactionEvent = (PlayerJoinFactionEvent) event;
			MessageType.PLAYER_JOIN_FACTION.sendMessage(playerJoinFactionEvent.getPlayer(), playerJoinFactionEvent.getPlayer().getFactionName());
		} else if(event instanceof PlayerLeaveFactionEvent) {
			PlayerLeaveFactionEvent playerLeaveFactionEvent = (PlayerLeaveFactionEvent) event;
			MessageType.PLAYER_LEAVE_FACTION.sendMessage(playerLeaveFactionEvent.getPlayer(), playerLeaveFactionEvent.getFaction().getName());
		} else if(event instanceof PlayerDeathEvent) {
			PlayerDeathEvent playerDeathEvent = (PlayerDeathEvent) event;
			if(playerDeathEvent.getDamager().isSegmentController()) {
				SegmentController segmentController = (SegmentController) playerDeathEvent.getDamager();
				String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
				String entityFactionName = (segmentController.getFactionId() != 0) ? segmentController.getFaction().getName() : "No Faction";
				MessageType.PLAYER_KILL_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), playerFactionName, segmentController.getRealName(), entityFactionName);
			} else if(playerDeathEvent.getDamager() instanceof PlayerState) {
				PlayerState killerState = (PlayerState) playerDeathEvent.getDamager();
				String killerFactionName = (killerState.getFactionId() != 0) ? killerState.getFactionName() : "No Faction";
				String killedFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
				if(killerState.equals(playerDeathEvent.getPlayer())) MessageType.PLAYER_SUICIDE_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), killedFactionName);
				else MessageType.PLAYER_KILL_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), killedFactionName, killerState.getName(), killerFactionName);
			} else {
				String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
				MessageType.PLAYER_DEATH_EVENT.sendMessage(playerDeathEvent.getPlayer().getName(), playerFactionName);
			}
		} else if(event instanceof FactionRelationChangeEvent) {
			FactionRelationChangeEvent factionRelationChangeEvent = (FactionRelationChangeEvent) event;
			if(((FactionRelationChangeEvent) event).getFrom().getIdFaction() <= 0 || ((FactionRelationChangeEvent) event).getTo().getIdFaction() <= 0) return;
			FactionRelation.RType oldRelation = factionRelationChangeEvent.getOldRelation();
			FactionRelation.RType newRelation = factionRelationChangeEvent.getNewRelation();
			switch(oldRelation) {
				case NEUTRAL:
					switch(newRelation) {
						case FRIEND:
							MessageType.FACTION_ALLY.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
							break;
						case ENEMY:
							MessageType.FACTION_DECLARE_WAR.sendMessage(factionRelationChangeEvent.getFrom().getName(), factionRelationChangeEvent.getTo().getName());
							break;
					}
					break;
				case ENEMY:
					switch(newRelation) {
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
					switch(newRelation) {
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

	public void registerUI(DiscordUI ui) {
		uiMap.put(ui.getClass().getSimpleName(), ui);
	}

	public PlayerData getLinkRequest(int linkCode) {
		return linkRequestMap.get(linkCode);
	}

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		MessageType.LOG_FATAL.sendMessage("Fatal Error", exception);
	}

	public void checkReset() {
		if(needsReset) resetBot();
	}
}
