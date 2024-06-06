package thederpgamer.starbridge.bot;

import api.common.GameServer;
import api.listener.events.Event;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.config.FileConfiguration;
import api.utils.game.PlayerUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.network.objects.ChatMessage;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.network.RegisteredClientOnServer;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.commands.*;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.ui.DiscordUI;
import thederpgamer.starbridge.utils.LogWatcher;

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
	private final long startTime;
	private final HashMap<String, DiscordUI> uiMap = new HashMap<>();
	private final HashMap<CommandData, DiscordCommand> commandMap = new HashMap<>();
	private final ConcurrentHashMap<Integer, PlayerData> linkRequestMap = new ConcurrentHashMap<>();

	private DiscordBot(StarBridge instance) {
		this.instance = instance;
		bot = createBot(ConfigManager.getMainConfig());
		Thread.setDefaultUncaughtExceptionHandler(this);
		initLogWatcher();
		(new Timer("channel_updater")).scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				updateChannelInfo();
			}
		}, 500, 5000);
		(new Thread(() -> {
			try {
				assert bot != null;
				bot.awaitReady();
				registerCommands();
				MessageType.SERVER_STARTING.sendMessage();
			} catch(InterruptedException exception) {
				StarBridge.getInstance().logException("Failed to register commands", exception);
			}
		})).start();
		startTime = System.currentTimeMillis();
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
			JDABuilder builder = JDABuilder.createDefault(config.getString("bot-token"));
			builder.addEventListeners(this);
			return builder.build();
		} catch(Exception exception) {
			instance.logException("An exception occurred while creating Discord bot (most likely due to invalid config)", exception);
			return null;
		}
	}

	public void updateChannelInfo() {
		try {
			int playerCount = GameServer.getServerState().getPlayerStatesByName().size();
			int playerMax = (int) ServerConfig.MAX_CLIENTS.getCurrentState();
			String chatChannelStats = ("Players: " + playerCount + " / " + playerMax);
			Objects.requireNonNull(bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"))).getManager().setTopic(chatChannelStats).queue();
			String logChannelStats = ("Clients: " + playerCount + " / " + playerMax + " \nCurrent Uptime: " + (System.currentTimeMillis() - startTime));
			Objects.requireNonNull(bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id"))).getManager().setTopic(logChannelStats).queue();
		} catch(Exception exception) {
			//LogManager.logException("Failed to update channel info", exception);
		}
	}

	private void registerCommands() {
		try {
			List<DiscordCommand> commandList = new ArrayList<>();
			commandList.add(new SettingsCommand());
			commandList.add(new PanelCommand());
			commandList.add(new HelpDiscordCommand());
			commandList.add(new InfoFactionCommand());
			commandList.add(new InfoPlayerCommand());
			commandList.add(new LinkCommand());
			commandList.add(new ListCommand());
			for(DiscordCommand command : commandList) {
				commandMap.put(command.getCommandData(), command);
				getGuild().upsertCommand(command.getCommandData()).queue();
			}
			MessageType.LOG_INFO.sendMessage("Commands loaded");
		} catch(Exception exception) {
			instance.logException("Failed to register commands", exception);
			exception.printStackTrace();
		}
	}

	public Guild getGuild() {
		return bot.getGuildById(ConfigManager.getMainConfig().getLong("server-id"));
	}

	public static void addToFilter(String exceptionClass) {
		removeFromFilter(exceptionClass);
		List<String> filter = getLoggingConfig().getList("ignored-exceptions");
		filter.add(exceptionClass);
		getLoggingConfig().set("ignored-exceptions", filter);
		getLoggingConfig().saveConfig();
	}

	public static void removeFromFilter(String exceptionClass) {
		List<String> filter = getLoggingConfig().getList("ignored-exceptions");
		filter.remove(exceptionClass);
		getLoggingConfig().set("ignored-exceptions", filter);
		getLoggingConfig().saveConfig();
	}

	private static FileConfiguration getLoggingConfig() {
		return ConfigManager.getLoggingConfig();
	}

	public static DiscordBot initialize(StarBridge instance) {
		return new DiscordBot(instance);
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
			for(CommandData commandData : commandMap.keySet()) {
				if(commandData.getName().equals(event.getName())) {
					commandMap.get(commandData).execute(event);
					return;
				}
			}
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String content = event.getMessage().getContentDisplay().trim();
		if(content.length() > 0) {
			if(!event.getAuthor().isBot() && !event.isWebhookMessage()) {
				if(event.getChannel().getIdLong() == ConfigManager.getMainConfig().getLong("chat-channel-id")) {
					if(content.charAt(0) != '/') sendServerMessage(event.getAuthor().getName(), content.trim());
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
		PlayerData playerData = ServerDatabase.getPlayerDataWithoutNull(playerState.getName());
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
										sendDiscordMessage(new MessageCreateBuilder().addContent("[" + chatMessage.sender + "]: " + message).build());
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
							sendDiscordMessage(new MessageCreateBuilder().addContent("[" + chatMessage.sender + "]: " + message).build());
							StarBridge.getInstance().logInfo(chatMessage.sender + " -> Public: " + message);
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
			MessageType.PLAYER_JOIN_FACTION.sendMessage(playerJoinFactionEvent.getPlayer().getName(), playerJoinFactionEvent.getFaction().getName());
		} else if(event instanceof PlayerLeaveFactionEvent) {
			PlayerLeaveFactionEvent playerLeaveFactionEvent = (PlayerLeaveFactionEvent) event;
			MessageType.PLAYER_LEAVE_FACTION.sendMessage(playerLeaveFactionEvent.getPlayer().getName(), playerLeaveFactionEvent.getFaction().getName());
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

	/**
	 * Sends a message to the Discord channel.
	 *
	 * @param message the message to send.
	 */
	public void sendDiscordMessage(MessageCreateData message) {
		try {
			Objects.requireNonNull(bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"))).sendMessage(message).queue();
		} catch(Exception exception) {
			instance.logException("An exception occurred while sending Discord message", exception);
		}
	}

	@Override
	public void uncaughtException(Thread thread, Throwable thrown) {
		if(!isFiltered(thrown)) {
			MessageType.LOG_EXCEPTION.sendMessage("An error has occurred in thread " + thread.getName(), thrown);
		}
	}

	public static boolean isFiltered(Throwable exception) {
		List<String> filter = getLoggingConfig().getList("ignored-exceptions");
		for(String exceptionClass : filter) {
			if(exception.getClass().getName().equals(exceptionClass)) return true;
		}
		return false;
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
}
