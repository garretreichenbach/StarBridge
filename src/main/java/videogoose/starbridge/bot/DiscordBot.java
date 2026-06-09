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
import org.schema.game.common.version.Version;
import org.schema.game.network.objects.ChatMessage;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.network.RegisteredClientOnServer;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.commands.CommandTypes;
import videogoose.starbridge.data.MessageData;
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

	private static final int ECHO_GUARD_CAPACITY = 50;

	private final StarBridge instance;
	private final JDA bot;
	private final ConcurrentHashMap<Integer, PlayerData> linkRequestMap = new ConcurrentHashMap<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
		var thread = new Thread(runnable, "starbridge-scheduler");
		thread.setDaemon(true);
		return thread;
	});

	/**
	 * Fixed-size echo guard: records recently injected bridge messages so the
	 * bridge can detect and drop re-relayed copies.  Backed by a
	 * {@link LinkedHashMap} with insertion-order iteration and an eldest-entry
	 * eviction policy that caps the set at {@link #ECHO_GUARD_CAPACITY} entries.
	 * Wrapped in {@link Collections#synchronizedSet} for thread safety.
	 */
	@SuppressWarnings("serial")
	private final Set<String> recentBridgeInjections = Collections.synchronizedSet(
			Collections.newSetFromMap(new LinkedHashMap<>() {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
					return size() > ECHO_GUARD_CAPACITY;
				}
			}));

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
		} catch (Exception exception) {
			instance.logException("An exception occurred while initialising Discord bot", exception);
		}
	}

	/** Shuts down the background scheduler. */
	public void shutdown() {
		scheduler.shutdownNow();
	}

	public static DiscordBot initialize(StarBridge instance) {
		var discordBot = new DiscordBot(instance);
		CommandTypes.registerDiscordCommands(discordBot);
		discordBot.initRestartTimer();
		return discordBot;
	}

	private void initRestartTimer() {
		long restartTimer = ConfigManager.getMainConfig().getLong("restart-timer");
		long shutdownTimer = ConfigManager.getMainConfig().getLong("default-shutdown-timer");
		assert restartTimer > shutdownTimer : "Restart timer must be greater than shutdown timer";
		if (restartTimer > 0) {
			if (shutdownTimer > 600000) {
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
			var builder = JDABuilder.createDefault(config.getString("bot-token"), Arrays.asList(GatewayIntent.values()));
			builder.addEventListeners(this);
			return builder.build();
		} catch (Exception exception) {
			instance.logException("An exception occurred while creating Discord bot", exception);
			return null;
		}
	}

	public void updateChannelInfo() {
		try {
			int playerCount = GameServer.getServerState().getPlayerStatesByName().size();
			int playerMax = (int) ServerConfig.MAX_CLIENTS.getCurrentState();
			var serverVersion = "v" + Version.VERSION + " (build " + Version.BUILD + ")";
			var chatChannelStats = "Players: " + playerCount + " / " + playerMax
					+ " \nVersion: " + serverVersion;
			var chatChannel = bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"));
			if (chatChannel == null) {
				instance.logWarning("chat-channel-id channel not found — skipping channel-info update");
				return;
			}
			queueAction(chatChannel.getManager().setTopic(chatChannelStats));

			var logChannelStats = "Clients: " + playerCount + " / " + playerMax
					+ " \nVersion: " + serverVersion
					+ " \nCurrent Uptime: " + (System.currentTimeMillis() - startTime);
			var logChannel = bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id"));
			if (logChannel == null) {
				instance.logWarning("log-channel-id channel not found — skipping channel-info update");
				return;
			}
			queueAction(logChannel.getManager().setTopic(logChannelStats));
		} catch (Exception exception) {
			StarBridge.getInstance().logException("Failed to update channel info", exception);
		}
	}

	public Guild getGuild() {
		return bot.getGuildById(ConfigManager.getMainConfig().getLong("server-id"));
	}

	public boolean hasRole(Member member, long roleId) {
		for (var role : member.getRoles()) {
			if (role.getIdLong() == roleId) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		var id = event.getComponentId();
		if (!id.startsWith(ErrorManager.BUTTON_PREFIX + ":")) return;
		if (!isStaff(event.getMember())) {
			event.reply("Only staff can manage error reports.").setEphemeral(true).queue();
			return;
		}
		var parts = id.split(":", 3);
		if (parts.length < 3) return;
		var action = parts[1];
		var fingerprint = parts[2];
		switch (action) {
			case "mute" -> {
				ErrorManager.muteFingerprint(fingerprint);
				event.reply("🔇 Muted error `" + fingerprint + "`. Future occurrences won't be posted.").setEphemeral(true).queue();
			}
			case "stack" -> {
				var entry = ErrorManager.getEntry(fingerprint);
				if (entry == null) event.reply("That error is no longer tracked.").setEphemeral(true).queue();
				else event.reply("```" + limit(entry.getFullText(), 1900) + "```").setEphemeral(true).queue();
			}
			case "pat" -> {
				var entry = ErrorManager.getEntry(fingerprint);
				String suggested = entry != null ? java.util.regex.Pattern.quote(entry.getExceptionClass()) : "";
				var input = TextInput.create("regex", TextInputStyle.SHORT)
						.setValue(suggested)
						.setPlaceholder("e.g. java\\.io\\.FileNotFoundException")
						.setRequired(true)
						.build();
				var modal = Modal.create(ErrorManager.BUTTON_PREFIX + ":patmodal", "Mute error pattern")
						.addComponents(Label.of("Regex to mute", input))
						.build();
				event.replyModal(modal).queue();
			}
		}
	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		if (!event.getModalId().equals(ErrorManager.BUTTON_PREFIX + ":patmodal")) return;
		if (!isStaff(event.getMember())) {
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
		if (s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	/**
	 * Sends a message (typically an error report with action buttons) to the log channel.
	 */
	public void sendToLogChannel(MessageCreateData data) {
		try {
			var channel = bot.getTextChannelById(ConfigManager.getMainConfig().getLong("log-channel-id"));
			if (channel != null) channel.sendMessage(data).queue();
		} catch (Exception ignored) {
			// Never let a reporting failure cascade.
		}
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		boolean canUse = true;
		var command = CommandTypes.getFromName(event.getName());
		if (event.getGuild() != null) {
			if (command != null) {
				if (command.permission.isAdminOnly()) {
					if (!hasRole(Objects.requireNonNull(event.getMember()), ConfigManager.getMainConfig().getLong("admin-role-id"))) {
						canUse = false;
					}
				} else {
					var playerData = ServerDatabase.getPlayerDataOrCreateIfNull(event.getUser().getName());
					Object permission = playerData.getPermission(command.getPermission().left);
					if (permission != null) {
						if (!permission.equals(command.getPermission().right)) {
							canUse = false;
						}
					} else if (playerData.inAnyGroup()) {
						for (PermissionGroup group : playerData.getGroups()) {
							permission = group.getPermission(command.getPermission().left);
							if (permission != null) {
								if (!permission.equals(command.getPermission().right)) {
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
		if (!canUse) {
			event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
		} else if (command != null) {
			try {
				command.execute(event);
			} catch (Exception exception) {
				instance.logException("An exception occurred while executing command /" + event.getName(), exception);
				if (!event.isAcknowledged()) event.reply("An error occurred while trying to execute this command.").setEphemeral(true).queue();
			}
		} else {
			event.reply("An error occurred while trying to execute this command.").setEphemeral(true).queue();
		}
	}

	/**
	 * Discord -> game path.
	 *
	 * <p>Filters:
	 * <ol>
	 *   <li>Ignore bot/webhook authors (existing guard).</li>
	 *   <li>Ignore messages whose content matches a recent bridge injection
	 *       (echo guard) — these are relays that the bridge itself sent to
	 *       Discord and that Discord is now echoing back.</li>
	 * </ol>
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		var content = event.getMessage().getContentDisplay().trim();
		if (content.isEmpty()) return;

		if (event.getAuthor().isBot() || event.isWebhookMessage()) return;

		if (event.getChannel().getIdLong() != ConfigManager.getMainConfig().getLong("chat-channel-id")) return;

		if (content.charAt(0) == '/') {
			event.getMessage().delete().queue();
			return;
		}

		// Echo-guard: drop any message whose content matches something the bridge
		// recently injected into the game — this is a relayed server announcement
		// bouncing back from Discord.
		if (recentBridgeInjections.contains(content)) {
			StarBridge.getInstance().logInfo("[DEBUG] drop bridge-echo from Discord: " + content);
			return;
		}

		// Build a DISCORD-origin envelope and relay to the game.
		var msg = MessageData.fromDiscord(event.getAuthor().getEffectiveName(), content);
		sendServerMessage(msg.author(), msg.content());
	}

	/**
	 * Sends a message attributed to a Discord user into the in-game chat and
	 * registers the injected text in the echo guard so that the resulting
	 * {@link api.listener.events.player.PlayerChatEvent} is not re-relayed to
	 * Discord.
	 *
	 * <p>The formatted string injected into the game is {@code "[sender] message"}.
	 * That same string is registered in the echo guard so that when the game
	 * fires a PlayerChatEvent for the injection, {@link #handleChannelMessage}
	 * recognises it and drops the relay.
	 *
	 * <p>Use {@link #sendBridgeMessage(String)} for server-synthesized
	 * announcements (join/leave, kills, etc.) that have no Discord-user sender.
	 *
	 * @param sender  display name shown in [brackets] before the message.
	 * @param message the raw text to broadcast.
	 */
	public void sendServerMessage(String sender, String message) {
		try {
			var formatted = (sender != null) ? "[" + sender + "] " + message : message;
			// Register in echo guard so the PlayerChatEvent fired by this injection
			// is not relayed back to Discord (Discord->game->Discord feedback loop).
			recentBridgeInjections.add(formatted);
			for (RegisteredClientOnServer client : GameServerState.instance.getClients().values()) {
				client.serverMessage(formatted);
			}
		} catch (Exception exception) {
			instance.logException("An exception occurred while sending server message", exception);
		}
	}

	/**
	 * Sends a BRIDGE-origin server announcement to the in-game chat and records
	 * the injected content in the echo guard so neither direction re-relays it.
	 *
	 * <p>Use this for all server-synthesized messages (join/leave notifications,
	 * faction events, kill messages, etc.) so the echo guard can recognise them
	 * when they arrive back from Discord or from a PlayerChatEvent.
	 *
	 * @param message the text to broadcast (without any sender prefix).
	 */
	public void sendBridgeMessage(String message) {
		// Register before injecting so the PlayerChatEvent / Discord echo is
		// already in the guard set by the time it arrives.
		recentBridgeInjections.add(message);

		try {
			for (RegisteredClientOnServer client : GameServerState.instance.getClients().values()) {
				client.serverMessage(message);
			}
		} catch (Exception exception) {
			instance.logException("An exception occurred while sending bridge message", exception);
		}
	}

	public void addLinkRequest(PlayerState playerState) {
		var playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerState.getName());
		removeLinkRequest(playerData);
		int num = (new Random()).nextInt(9999 - 1000) + 1000;
		linkRequestMap.put(num, playerData);
		var chatChannel = bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"));
		String channelName = (chatChannel != null) ? chatChannel.getName() : "discord";
		PlayerUtils.sendMessage(playerState, "Use /link " + num + " in #" + channelName + " to link your account. This code will expire in 15 minutes.");
		scheduler.schedule(() -> removeLinkRequest(playerData), 15, TimeUnit.MINUTES);
	}

	public void removeLinkRequest(PlayerData playerData) {
		for (Integer key : linkRequestMap.keySet()) {
			if (linkRequestMap.get(key).equals(playerData)) {
				linkRequestMap.remove(key);
				return;
			}
		}
	}

	/**
	 * Game -> Discord + game-internal routing.
	 *
	 * <p>The PlayerChatEvent case is the only path that relays game messages to
	 * Discord.  Before relaying it checks the echo guard — if the message
	 * matches a recent bridge injection it was likely a server announcement
	 * printed in-game and picked up by the chat listener (i.e. a feedback loop).
	 */
	public void handleEvent(Event event) {
		if (!event.isServer()) return;
		switch (event) {
			case PlayerCustomCommandEvent playerCustomCommandEvent -> {
				if (playerCustomCommandEvent.getCommand().isAdminOnly() && !playerCustomCommandEvent.getSender().isAdmin()) {
					return;
				}
				StarBridge.getInstance().logInfo(
						playerCustomCommandEvent.getSender().getName()
								+ " executed command: "
								+ playerCustomCommandEvent.getCommand().getCommand());
			}
			case PlayerChatEvent playerChatEvent -> handlePlayerChatEvent(playerChatEvent);
			case PlayerJoinWorldEvent playerJoinWorldEvent -> {
				var pd = ServerDatabase.getPlayerData(playerJoinWorldEvent.getPlayerName());
				if (pd == null) {
					ServerDatabase.addNewPlayerData(playerJoinWorldEvent.getPlayerName());
					MessageType.NEW_PLAYER_JOIN.sendMessage(playerJoinWorldEvent.getPlayerName());
				} else {
					MessageType.PLAYER_JOIN.sendMessage(playerJoinWorldEvent.getPlayerName());
				}
			}
			case PlayerLeaveWorldEvent playerLeaveWorldEvent ->
					MessageType.PLAYER_LEAVE.sendMessage(playerLeaveWorldEvent.getPlayerName());
			case FactionCreateEvent factionCreateEvent -> {
				var player = factionCreateEvent.getPlayer();
				var faction = factionCreateEvent.getFaction();
				String playerName = (player != null) ? player.getName() : "<unknown>";
				String factionName = (faction != null) ? faction.getName() : "<unknown>";
				MessageType.FACTION_CREATE.sendMessage(playerName, factionName);
			}
			case PlayerJoinFactionEvent playerJoinFactionEvent -> {
				var player = playerJoinFactionEvent.getPlayer();
				String playerName = (player != null) ? player.toString() : "<unknown>";
				String factionName = (player != null) ? player.getFactionName() : "<unknown>";
				if (factionName == null) {
					factionName = "<unknown>";
					instance.logWarning("PlayerJoinFactionEvent: null factionName for player " + playerName);
				}
				MessageType.PLAYER_JOIN_FACTION.sendMessage(playerName, factionName);
			}
			case PlayerLeaveFactionEvent playerLeaveFactionEvent -> {
				var player = playerLeaveFactionEvent.getPlayer();
				var faction = playerLeaveFactionEvent.getFaction();
				String playerName = (player != null) ? player.toString() : "<unknown>";
				String factionName = (faction != null) ? faction.getName() : "<unknown>";
				MessageType.PLAYER_LEAVE_FACTION.sendMessage(playerName, factionName);
			}
			case PlayerDeathEvent playerDeathEvent -> {
				if (playerDeathEvent.getDamager().isSegmentController()) {
					var sc = (SegmentController) playerDeathEvent.getDamager();
					var player = playerDeathEvent.getPlayer();
					String pFaction = (player != null && player.getFactionId() != 0) ? player.getFactionName() : "No Faction";
					String eFaction = (sc.getFactionId() != 0) ? sc.getFaction().getName() : "No Faction";
					String pName = (player != null) ? player.getName() : "<unknown>";
					MessageType.PLAYER_KILL_EVENT.sendMessage(pName, pFaction, sc.getRealName(), eFaction);
				} else if (playerDeathEvent.getDamager() instanceof PlayerState killerState) {
					var player = playerDeathEvent.getPlayer();
					String killerFaction = (killerState.getFactionId() != 0) ? killerState.getFactionName() : "No Faction";
					String killedFaction = (player != null && player.getFactionId() != 0) ? player.getFactionName() : "No Faction";
					String killedName = (player != null) ? player.getName() : "<unknown>";
					if (killerState.equals(player)) {
						MessageType.PLAYER_SUICIDE_EVENT.sendMessage(killedName, killedFaction);
					} else {
						MessageType.PLAYER_KILL_EVENT.sendMessage(killedName, killedFaction, killerState.getName(), killerFaction);
					}
				} else {
					var player = playerDeathEvent.getPlayer();
					String pFaction = (player != null && player.getFactionId() != 0) ? player.getFactionName() : "No Faction";
					String pName = (player != null) ? player.getName() : "<unknown>";
					MessageType.PLAYER_DEATH_EVENT.sendMessage(pName, pFaction);
				}
			}
			case FactionRelationChangeEvent factionRelationChangeEvent -> {
				if (factionRelationChangeEvent.getFrom().getIdFaction() <= 0
						|| factionRelationChangeEvent.getTo().getIdFaction() <= 0) return;
				var from = factionRelationChangeEvent.getFrom();
				var to = factionRelationChangeEvent.getTo();
				String fromName = (from != null) ? from.getName() : "<unknown>";
				String toName = (to != null) ? to.getName() : "<unknown>";
				var oldRelation = factionRelationChangeEvent.getOldRelation();
				var newRelation = factionRelationChangeEvent.getNewRelation();
				switch (oldRelation) {
					case NEUTRAL -> {
						switch (newRelation) {
							case FRIEND -> MessageType.FACTION_ALLY.sendMessage(fromName, toName);
							case ENEMY -> MessageType.FACTION_DECLARE_WAR.sendMessage(fromName, toName);
							default -> { /* no action */ }
						}
					}
					case ENEMY -> {
						switch (newRelation) {
							case NEUTRAL -> MessageType.FACTION_PEACE.sendMessage(fromName, toName);
							case FRIEND -> {
								MessageType.FACTION_PEACE.sendMessage(fromName, toName);
								MessageType.FACTION_ALLY.sendMessage(fromName, toName);
							}
							default -> { /* no action */ }
						}
					}
					case FRIEND -> {
						switch (newRelation) {
							case NEUTRAL -> MessageType.FACTION_CANCEL_ALLY.sendMessage(fromName, toName);
							case ENEMY -> {
								MessageType.FACTION_CANCEL_ALLY.sendMessage(fromName, toName);
								MessageType.FACTION_DECLARE_WAR.sendMessage(fromName, toName);
							}
							default -> { /* no action */ }
						}
					}
				}
			}
			default -> { /* unhandled event type */ }
		}
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Handles the PlayerChatEvent — the single game->Discord relay path.
	 *
	 * <p>Public-channel messages are forwarded to Discord as GAME-origin envelopes.
	 * The echo guard is checked first: if the text matches a recent bridge
	 * injection the event is a feedback echo and is silently dropped.
	 */
	private void handlePlayerChatEvent(PlayerChatEvent playerChatEvent) {
		var rawMessage = playerChatEvent.getMessage();
		String message = rawMessage.text
				.replace("@", "")
				.replace("\"", "");
		var chatMessage = new ChatMessage(rawMessage);
		var playerData = ServerDatabase.getPlayerDataOrCreateIfNull(rawMessage.sender);

		try {
			switch (chatMessage.receiverType) {
				case SYSTEM:
					StarBridge.getInstance().logInfo(chatMessage.sender + ": " + message);
					break;
				case DIRECT:
					if (chatMessage.receiver != null) {
						StarBridge.getInstance().logInfo(
								chatMessage.sender + " -> " + chatMessage.receiver + ": " + message);
					}
					break;
				case CHANNEL:
					handleChannelMessage(chatMessage, message, playerData);
					break;
			}
		} catch (Exception exception) {
			StarBridge.getInstance().logException(
					"An exception occurred while trying to handle a chat event", exception);
		}
	}

	/**
	 * Routes CHANNEL-type chat messages.  Only PUBLIC-channel messages are
	 * relayed to Discord; all others are logged internally.
	 *
	 * <p>Echo-guard check: if the message content was recently injected by the
	 * bridge it must not be forwarded — doing so would create a duplicate.
	 */
	private void handleChannelMessage(ChatMessage chatMessage, String message, PlayerData playerData) {
		if (chatMessage.getChannel() != null) {
			String messageToSend;
			try {
				switch (chatMessage.getChannel().getType()) {
					case FACTION: {
						int factionId = Integer.parseInt(
								chatMessage.receiver.substring(chatMessage.receiver.indexOf("Faction") + 7));
						String factionName = GameServer.getServerState()
								.getFactionManager().getFaction(factionId).getName();
						messageToSend = chatMessage.sender + " -> [" + factionName + "]: " + message;
						break;
					}
					case PUBLIC: {
						messageToSend = chatMessage.sender + " -> Public: " + message;
						// Echo-guard: do not relay messages that the bridge itself injected.
						if (recentBridgeInjections.contains(message)) {
							StarBridge.getInstance().logInfo(
									"[DEBUG] drop bridge-echo from game: " + message);
							StarBridge.getInstance().logInfo(messageToSend);
							return;
						}
						// Single game->Discord relay path; build a GAME-origin envelope.
						var envelope = MessageData.fromGame(chatMessage.sender, message);
						try {
							sendDiscordMessage(new MessageCreateBuilder()
									.addContent("[" + envelope.author() + "]: " + envelope.content())
									.build());
						} catch (Exception exception) {
							instance.logException(
									"An exception occurred while trying to handle a chat event", exception);
						}
						break;
					}
					case PARTY: {
						messageToSend = chatMessage.sender + " -> Party: " + message;
						break;
					}
					default: {
						messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
						break;
					}
				}
			} catch (Exception ignored) {
				messageToSend = chatMessage.sender + " -> " + chatMessage.receiver + ": " + message;
			}
			StarBridge.getInstance().logInfo(messageToSend);
		} else if (chatMessage.receiver != null
				&& chatMessage.receiver.toLowerCase(java.util.Locale.ENGLISH).startsWith("faction")) {
			try {
				int factionId = Integer.parseInt(
						chatMessage.receiver.substring(chatMessage.receiver.indexOf("Faction") + 7));
				String factionName = GameServer.getServerState()
						.getFactionManager().getFaction(factionId).getName();
				StarBridge.getInstance().logInfo(
						chatMessage.sender + " -> [" + factionName + "]: " + message);
			} catch (Exception ignored) {
				StarBridge.getInstance().logInfo(chatMessage.sender + " -> [Unknown Faction]: " + message);
			}
		} else if ("all".equalsIgnoreCase(chatMessage.receiver)) {
			// Echo-guard for the "all" receiver path as well.
			if (recentBridgeInjections.contains(message)) {
				StarBridge.getInstance().logInfo("[DEBUG] drop bridge-echo from game (all): " + message);
				return;
			}
			var envelope = MessageData.fromGame(chatMessage.sender, message);
			sendDiscordMessage(new MessageCreateBuilder()
					.addContent("[" + envelope.author() + "]: " + envelope.content())
					.build());
		}
	}

	public void setBotToUser(User user) {
		try {
			@SuppressWarnings("unused")
			AccountManager manager = bot.getSelfUser().getManager();
			needsReset = true;
		} catch (Exception exception) {
			instance.logException("An exception occurred while setting bot to user", exception);
		}
	}

	private void queueAction(RestAction<?> action) {
		action.queue();
	}

	public void resetBot() {
		try {
			@SuppressWarnings("unused")
			AccountManager manager = bot.getSelfUser().getManager();
			needsReset = false;
		} catch (Exception exception) {
			instance.logException("An exception occurred while resetting bot", exception);
		}
	}

	public String getURL(String string) {
		if (string.startsWith("\"") && string.endsWith("\"")) string = string.substring(1, string.length() - 1);
		if (!string.startsWith("https://")) string = "https://" + string;
		return string;
	}

	private Icon getAvatar(String avatarUrl) {
		try {
			avatarUrl = getURL(avatarUrl);
			var connection = new URL(avatarUrl).openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			try (InputStream inputStream = connection.getInputStream()) {
				return Icon.from(inputStream);
			}
		} catch (Exception exception) {
			instance.logException("An exception occurred while setting bot avatar", exception);
			return null;
		}
	}

	/**
	 * Single Discord->Discord relay path: sends a message to the configured
	 * chat channel and also registers its plain-text content in the echo guard
	 * so that if the same text arrives back via a PlayerChatEvent it is dropped.
	 *
	 * @param message the JDA message payload to send.
	 */
	public void sendDiscordMessage(MessageCreateData message) {
		try {
			var channel = bot.getTextChannelById(ConfigManager.getMainConfig().getLong("chat-channel-id"));
			if (channel == null) {
				instance.logWarning("sendDiscordMessage: chat-channel-id channel not found");
				return;
			}
			// Register plain-text content so the echo guard can drop it if it
			// arrives back as a PlayerChatEvent.
			String content = message.getContent();
			if (content != null && !content.isEmpty()) {
				recentBridgeInjections.add(content);
			}
			queueAction(channel.sendMessage(message));
		} catch (Exception exception) {
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
		if (needsReset) resetBot();
	}
}
