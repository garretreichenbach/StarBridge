package thederpgamer.starbridge.bot;

import api.common.GameServer;
import api.listener.events.Event;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.mod.config.PersistentObjectUtil;
import api.utils.StarRunnable;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.chat.ChannelRouter;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.network.objects.ChatMessage;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.bot.runnable.BotThread;
import thederpgamer.starbridge.bot.runnable.DiscordMessageRunnable;
import thederpgamer.starbridge.bot.runnable.ServerMessageRunnable;
import thederpgamer.starbridge.commands.DiscordCommand;
import thederpgamer.starbridge.data.exception.ExceptionData;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.LogManager;
import thederpgamer.starbridge.server.ServerDatabase;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls discord bot.
 */
public class StarBot extends ListenerAdapter {

	private final String newPlayerMessage = ":confetti_ball: Everybody welcome %PLAYER_NAME% to %SERVER_NAME%";

	private static StarBot instance;
	private final String token;
	private final DiscordWebhook chatWebhook;
	private final long chatChannelId;
	private final DiscordWebhook logWebhook;
	private final long logChannelId;
	private final ConcurrentHashMap<Integer, PlayerData> linkRequestMap = new ConcurrentHashMap<>();

	public static StarBot getInstance() {
		return instance;
	}

	private final BotThread botThread;

	public StarBot() {
		instance = this;
		try {
			botThread = new BotThread(ConfigManager.getMainConfig(), this);
			botThread.start();
			token = ConfigManager.getMainConfig().getString("bot-token");
			chatWebhook = new DiscordWebhook("https://" + ConfigManager.getMainConfig().getString("chat-webhook"));
			chatChannelId = ConfigManager.getMainConfig().getLong("chat-channel-id");
			logWebhook = new DiscordWebhook("https://" + ConfigManager.getMainConfig().getString("log-webhook"));
			logChannelId = ConfigManager.getMainConfig().getLong("log-channel-id");
			sendDiscordMessage(":white_check_mark: Server Started");
			loadDonators();
		} catch(LoginException e) {
			throw new RuntimeException(e);
		}
	}

	private void loadDonators() {
		try {
			File donatorsFile = new File("donators.smdat");
			if(!donatorsFile.exists()) donatorsFile.createNewFile();
			FileOutputStream outputStream = new FileOutputStream(donatorsFile);
			ArrayList<Object> donators = PersistentObjectUtil.getObjects(StarBridge.getInstance().getSkeleton(), PlayerData.class);
			for(int i  = 0; i < donators.size(); i ++) {
				PlayerData playerData = (PlayerData) donators.get(i);
				if(playerData.getDiscordId() > 0) {
					int finalI = i;
					botThread.bot.retrieveUserById(playerData.getDiscordId()).queue(user -> {
						if(user != null) {
							try {
								if(hasRole((Member) user, 1055652219497758725L)) {
									String donatorData = playerData.getPlayerName() + " | " + playerData.getDiscordId() + " | Explorer";
									outputStream.write((donatorData + ", ").getBytes());
								} else if(hasRole((Member) user, 1055656604256706564L)) {
									String donatorData = playerData.getPlayerName() + " | " + playerData.getDiscordId() + " | Captain";
									outputStream.write((donatorData + ", ").getBytes());
								} else if(hasRole((Member) user, 618955319367696384L)) {
									String donatorData = playerData.getPlayerName() + " | " + playerData.getDiscordId() + " | Staff";
									outputStream.write((donatorData + ", ").getBytes());
								}
								if(finalI == donators.size() - 1) {
									outputStream.flush();
									outputStream.close();
								}
							} catch(IOException exception) {
								exception.printStackTrace();
							}
						}
					});
				}
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	public void handleEvent(Event event) {
		if(event instanceof PlayerCustomCommandEvent) {
			PlayerCustomCommandEvent playerCustomCommandEvent = (PlayerCustomCommandEvent) event;
			if(playerCustomCommandEvent.getCommand().isAdminOnly() && !playerCustomCommandEvent.getSender().isAdmin()) return;
			LogManager.logCommand(playerCustomCommandEvent.getSender().getName(), playerCustomCommandEvent.getFullLine());
		} else if(event instanceof PlayerChatEvent) {
			PlayerChatEvent playerChatEvent = (PlayerChatEvent) event;
			String message = playerChatEvent.getMessage().text;
			message = message.replace("@", "");
			message = message.replace("\"", "");
			if(botThread.lastMessage.equals(message)) return;
			ChatMessage chatMessage = new ChatMessage(playerChatEvent.getMessage());
			PlayerData playerData = ServerDatabase.getPlayerDataWithoutNull(chatMessage.sender);
			try {
				if (chatMessage.receiverType.equals(ChatMessage.ChatMessageType.CHANNEL)) {
					if (chatMessage.getChannel() != null && chatMessage.getChannel().getType().equals(ChannelRouter.ChannelType.FACTION)) {
						LogManager.logChat(chatMessage, "FACTION");
					} else if (chatMessage.getChannel() == null && chatMessage.receiver.toLowerCase().equals("all") || (chatMessage.getChannel().getType().equals(ChannelRouter.ChannelType.PUBLIC) && !chatMessage.getChannel().hasPassword() && !chatMessage.getChannel().getType().equals(ChannelRouter.ChannelType.FACTION))) {
						sendDiscordMessage(playerData.getPlayerName(), message);
						LogManager.logChat(chatMessage, "GENERAL");
					}
				}
			} catch(Exception exception) {
				exception.printStackTrace();
			}
			botThread.lastMessage = message;
		} else if(event instanceof PlayerJoinWorldEvent) {
			PlayerJoinWorldEvent playerJoinWorldEvent = (PlayerJoinWorldEvent) event;
			ServerDatabase.getPlayerDataWithoutNull(playerJoinWorldEvent.getPlayerName());
			String playerJoinMessage = ":arrow_right: %PLAYER_NAME% has joined the server";
			sendBotEventMessage(playerJoinMessage, playerJoinWorldEvent.getPlayerName());
			checkAlt(((PlayerJoinWorldEvent) event).getPlayerName());
		} else if(event instanceof PlayerLeaveWorldEvent) {
			//updateChannelInfo();
			String playerLeaveMessage = ":door: %PLAYER_NAME% has left the server";
			sendBotEventMessage(playerLeaveMessage, ((PlayerLeaveWorldEvent) event).getPlayerName());
		} else if(event instanceof FactionCreateEvent) {
			FactionCreateEvent factionCreateEvent = (FactionCreateEvent) event;
			String createFactionMessage = ":new: %PLAYER_NAME% has created a new faction called %FACTION_NAME%";
			sendBotEventMessage(createFactionMessage, factionCreateEvent.getPlayer().getName(), factionCreateEvent.getFaction().getName());
		} else if(event instanceof PlayerJoinFactionEvent) {
			PlayerJoinFactionEvent playerJoinFactionEvent = (PlayerJoinFactionEvent) event;
			String factionJoinMessage = ":heavy_plus_sign: %PLAYER_NAME% has joined %FACTION_NAME%";
			sendBotEventMessage(factionJoinMessage, playerJoinFactionEvent.getPlayer().getName(), playerJoinFactionEvent.getFaction().getName());
		} else if(event instanceof PlayerLeaveFactionEvent) {
			PlayerLeaveFactionEvent playerLeaveFactionEvent = (PlayerLeaveFactionEvent) event;
			String factionLeaveMessage = ":heavy_minus_sign: %PLAYER_NAME% has left %FACTION_NAME%";
			sendBotEventMessage(factionLeaveMessage, playerLeaveFactionEvent.getPlayer().getName(), playerLeaveFactionEvent.getFaction().getName());
		} else if(event instanceof PlayerDeathEvent) {
			PlayerDeathEvent playerDeathEvent = (PlayerDeathEvent) event;
			if(playerDeathEvent.getDamager().isSegmentController()) {
				SegmentController segmentController = (SegmentController) playerDeathEvent.getDamager();
				String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
				String entityFactionName = (segmentController.getFactionId() != 0) ? segmentController.getFaction().getName() : "No Faction";
				String playerKillByEntityMessage = ":skull_crossbones: %PLAYER_NAME% %PLAYER_FACTION_NAME% was slain by %ENTITY_NAME% %ENTITY_FACTION_NAME%";
				sendBotEventMessage(playerKillByEntityMessage, playerDeathEvent.getPlayer().getName(), playerFactionName, segmentController.getRealName(), entityFactionName);
			} else if(playerDeathEvent.getDamager() instanceof PlayerState) {
				PlayerState killerState = (PlayerState) playerDeathEvent.getDamager();
				String killerFactionName = (killerState.getFactionId() != 0) ? killerState.getFactionName() : "No Faction";
				String killedFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
				String playerKillByPlayerMessage = ":skull_crossbones: %PLAYER_NAME_1% %PLAYER_1_FACTION_NAME% was slain by %PLAYER_NAME_2% %PLAYER_2_FACTION_NAME%";
				if(killerState.equals(playerDeathEvent.getPlayer())) sendDiscordMessage("Player " + killerState.getName() + " [" + killerFactionName + "] took their own life.");
				else sendBotEventMessage(playerKillByPlayerMessage, playerDeathEvent.getPlayer().getName(), killedFactionName, killerState.getName(), killerFactionName);
			} else {
				String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
				String playerKillByOtherMessage = ":skull_crossbones: %PLAYER_NAME% %PLAYER_FACTION_NAME% has died";
				sendBotEventMessage(playerKillByOtherMessage, playerDeathEvent.getPlayer().getName(), playerFactionName);
			}
		} else if(event instanceof FactionRelationChangeEvent) {
			FactionRelationChangeEvent factionRelationChangeEvent = (FactionRelationChangeEvent) event;
			if(factionRelationChangeEvent.getNewRelation().equals(FactionRelation.RType.FRIEND)) {
				String factionAllyMessage = ":shield: %FACTION_NAME_1% is now allied with %FACTION_NAME_2%";
				sendBotEventMessage(factionAllyMessage, factionRelationChangeEvent.getTo().getName(), factionRelationChangeEvent.getFrom().getName());
			} else if(factionRelationChangeEvent.getNewRelation().equals(FactionRelation.RType.ENEMY)) {
				String factionWarMessage = ":crossed_swords: %FACTION_NAME_1% is now at war with %FACTION_NAME_2%";
				sendBotEventMessage(factionWarMessage, factionRelationChangeEvent.getTo().getName(), factionRelationChangeEvent.getFrom().getName());
			}
		}
	}

	private void checkAlt(String playerName) {
		new StarRunnable() {
			@Override
			public void run() {
				try {
					PlayerState player = GameServer.getServerState().getPlayerFromNameIgnoreCase(playerName);
					if(!player.isAdmin()) {
						PlayerData playerData = ServerDatabase.getPlayerData(playerName);
						if(playerData != null) {
							playerData.setStarMadeName(playerName);
							playerData.setIP(player.getIp());
							if(isVPN(player.getIp())) {
								getInstance().sendDiscordMessage(":clown: Player " + playerData.getPlayerName() + " attempted to use a VPN. Laugh at this user!");
								GameServer.getServerState().getController().sendLogout(player.getClientId(), "This server does not allow VPNs.");
								return;
							}
							if(playerData.isExempt()) return;
							for(PlayerData playerData1 : ServerDatabase.getAllPlayerData()) {
								if((playerData.getStarmadeName().equals(playerData1.getStarmadeName()) || playerData1.getIP().equals(playerData.getIP())) && !playerData1.getPlayerName().equals(playerData.getPlayerName())) {
									if(ConfigManager.getMainConfig().getConfigurableBoolean("kick-non-admin-alts", true)) {
										getInstance().sendDiscordMessage(":clown: Player " + playerData1.getPlayerName() + " attempted to log-in as " + playerData.getPlayerName() + " but the server doesn't allow alts!");
										GameServer.getServerState().getController().sendLogout(player.getClientId(), "This server does not allow alternative accounts.");
									}
								}
							}
						}
					}
				} catch(Exception exception) {
					exception.printStackTrace();
				}
			}
		}.runLater(StarBridge.getInstance(), 30);
	}

	private boolean isVPN(String ip) {
		ip = ip.substring(0, ip.lastIndexOf(':'));
		String url = "http://check.getipintel.net/check.php?ip=" + ip;
		try {
			String response = new BufferedReader(new InputStreamReader(new URL(url).openStream())).readLine();
			return Double.parseDouble(response) > 0.9;
		} catch(Exception exception) {
			exception.printStackTrace();
			return false;
		}
	}

	private void sendBotEventMessage(String message, String... args) {
		if(args != null && args.length > 0) {
			StringBuilder builder = new StringBuilder();
			int argIndex = 0;
			int argsCount = 0;
			for(char c : message.toCharArray()) {
				if(c == '%') argsCount ++;
				builder.append(c);
			}
			message = builder.toString();
			String[] words = message.split(" ");
			builder = new StringBuilder();
			if(args.length == argsCount / 2) {
				for(int i = 0; i < words.length; i ++) {
					if(i != 0) builder.append(" ");
					String word = words[i];
					if(word.charAt(1) == '%' && word.charAt(word.length() - 2) == '%') {
						builder.append('[').append(args[argIndex]).append(']');
						argIndex ++;
					} else if(word.charAt(0) == '%' && word.charAt(word.length() - 1) == '%') {
						builder.append(args[argIndex]);
						argIndex ++;
					} else builder.append(word);
				}
				try {
					sendDiscordMessage(builder.toString().trim());
					sendServerMessage(ConfigManager.getMainConfig().getString("bot-name"), builder.toString().trim());
				} catch(Exception exception) {
					LogManager.logException("An exception encountered while trying to send a message from the bot", exception);
				}
			} else LogManager.logWarning("Invalid message arguments count! Should be " + argsCount / 2 + " arguments but only found " + args.length + ".", null);
		}
	}

	public void sendServerMessage(String message) {
		botThread.queue(new ServerMessageRunnable(message));
	}

	public void sendServerMessage(String sender, String message) {
		botThread.queue(new ServerMessageRunnable(sender, message));
	}

	public void sendDiscordMessage(String message) {
		botThread.queue(new DiscordMessageRunnable(message));
	}

	public void sendDiscordMessage(String sender, String message) {
		botThread.queue(new DiscordMessageRunnable(sender, message));
	}

	public String getToken() {
		return token;
	}

	public DiscordWebhook getChatWebhook() {
		return chatWebhook;
	}

	public long getChatChannelId() {
		return chatChannelId;
	}

	public DiscordWebhook getLogWebhook() {
		return logWebhook;
	}

	public long getLogChannelId() {
		return logChannelId;
	}

	public ConcurrentHashMap<Integer, PlayerData> getLinkRequestMap() {
		return linkRequestMap;
	}

	public BotThread getBotThread() {
		return botThread;
	}

	public void addLinkRequest(PlayerState playerState) {
		final PlayerData playerData = ServerDatabase.getPlayerDataWithoutNull(playerState.getName());
		removeLinkRequest(playerData);
		int num = (new Random()).nextInt(9999 - 1000) + 1000;
		linkRequestMap.put(num, playerData);
		PlayerUtils.sendMessage(playerState, "Use /link " + num + " in #" + botThread.bot.getTextChannelById(chatChannelId).getName() + " to link your account. This code will expire in 15 minutes.");
		new Timer("link_timer_" + linkRequestMap.get(num).getPlayerName()).schedule(new TimerTask() {
			@Override
			public void run() {
				removeLinkRequest(playerData);
			}
		}, 900000);
	}

	public boolean hasRole(Member member, long roleId) {
		for(Role role : member.getRoles()) if(role.getIdLong() == roleId) return true;
		return false;
	}

	public void removeLinkRequest(PlayerData playerData) {
		for(Integer key : linkRequestMap.keySet()) {
			if(linkRequestMap.get(key).equals(playerData)) {
				linkRequestMap.remove(key);
				return;
			}
		}
	}

	public PlayerData getLinkRequest(int linkCode) {
		return linkRequestMap.get(linkCode);
	}

	public void resetWebhook() {
		chatWebhook.setUsername(ConfigManager.getMainConfig().getString("bot-name"));
		chatWebhook.setAvatarUrl("https://" + ConfigManager.getMainConfig().getString("bot-avatar"));
		chatWebhook.setContent("");
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if(event.getGuild() != null) {
			CommandInterface commandInterface = StarLoader.getCommand(event.getName());
			if(commandInterface != null) {
				if(commandInterface instanceof DiscordCommand) {
					if(commandInterface.isAdminOnly() && !hasRole(Objects.requireNonNull(event.getMember()), ConfigManager.getMainConfig().getLong("admin-role-id"))) {
						event.reply("You don't have permission to use this command!").queue();
						return;
					}
					((DiscordCommand) commandInterface).execute(event);
				} else event.reply("This command is only available in-game").queue();
			} else event.reply("/" + event.getCommandPath().replace("/", " ") + " is not a valid command").queue();
		}
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String content = event.getMessage().getContentDisplay().trim();
		if(content.length() > 0) {
			if(!event.getAuthor().isBot() && !event.isWebhookMessage()) {
				if(event.getChannel().getIdLong() == chatChannelId) {
					if(content.charAt(0) != '/') sendServerMessage(event.getAuthor().getName(), content.trim());
					else event.getMessage().delete().queue();
				}
			}
		}
	}

	public void logException(String exceptionMessage) {
		logWebhook.setUsername(getBotThread().bot.getSelfUser().getName());
		logWebhook.setAvatarUrl(getBotThread().bot.getSelfUser().getAvatarUrl());
		String message = "<@&" + ConfigManager.getMainConfig().getLong("admin-role-id") + ">An exception has occurred:\n";
		logWebhook.setContent(message);
		logWebhook.addEmbed(new DiscordWebhook.EmbedObject().setDescription("```" + exceptionMessage + "```"));
		try {
			logWebhook.execute();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		resetWebhook();
	}

	public void logException(String line, String[] stacktraceLines) {
		logWebhook.setUsername(getBotThread().bot.getSelfUser().getName());
		logWebhook.setAvatarUrl(getBotThread().bot.getSelfUser().getAvatarUrl());
		//String message = "<@&" + ConfigManager.getMainConfig().getLong("admin-role-id") + ">\n" + line;
		logWebhook.setContent(line);
		logWebhook.addEmbed(new DiscordWebhook.EmbedObject().setDescription("```" + Arrays.asList(stacktraceLines) + "```"));
		try {
			logWebhook.execute();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
		resetWebhook();
	}

	public void log(String line) {
		logWebhook.setUsername(getBotThread().getName());
		logWebhook.setAvatarUrl(getBotThread().bot.getSelfUser().getAvatarUrl());
		logWebhook.setContent(line);
		try {
			logWebhook.execute();
		} catch(IOException e) {
			e.printStackTrace();
		}
		resetWebhook();
	}
}
