package thederpgamer.starbridge.server.bot;

import api.common.GameClient;
import api.common.GameCommon;
import api.listener.events.Event;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.network.objects.ChatMessage;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.config.ConfigFile;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.DiscordWebhook;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.server.commands.DiscordCommand;
import thederpgamer.starbridge.utils.DataUtils;
import thederpgamer.starbridge.utils.LogUtils;
import thederpgamer.starbridge.utils.MessageType;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.*;

/**
 * DiscordBot.java
 * <Description>
 *
 * @since 03/11/2021
 * @author TheDerpGamer
 */
public class DiscordBot extends ListenerAdapter {

    //Data
    public JDA bot;
    private String token;
    private DiscordWebhook chatWebhook;
    private long channelId;
    private HashMap<PlayerData, Integer> linkRequestMap;
    private ChatMessage lastMessage;

    //Config
    public ConfigFile messageConfig;
    private String serverStartMessage = ":white_check_mark: Server Started";
    private String serverStopMessage = ":octagonal_sign: Server Stopped";
    private String newPlayerMessage = ":confetti_ball: Everybody welcome %PLAYER_NAME% to %SERVER_NAME%";
    private String playerJoinMessage = ":arrow_right: %PLAYER_NAME% has joined the server";
    private String playerLeaveMessage = ":door: %PLAYER_NAME% has left the server";
    private String createFactionMessage = ":new: %PLAYER_NAME% has created a new faction called %FACTION_NAME%";
    private String factionJoinMessage = ":heavy_plus_sign: %PLAYER_NAME% has joined %FACTION_NAME%";
    private String factionLeaveMessage = ":heavy_minus_sign: %PLAYER_NAME% has left %FACTION_NAME%";
    private String factionAllyMessage = ":shield: %FACTION_NAME_1% is now allied with %FACTION_NAME_2%";
    private String factionWarMessage = ":crossed_swords: %FACTION_NAME_1% is now at war with %FACTION_NAME_2%";
    private String playerKillByPlayerMessage = ":skull_crossbones: %PLAYER_NAME_1% [%PLAYER_1_FACTION_NAME%] was slain by %PLAYER_NAME_2% [%PLAYER_2_FACTION_NAME%]";
    private String playerKillByEntityMessage = ":skull_crossbones: %PLAYER_NAME% [%PLAYER_FACTION_NAME%] was slain by %ENTITY_NAME% [%ENTITY_FACTION_NAME%]";
    private String playerKillByOtherMessage = ":skull_crossbones: %PLAYER_NAME% [%PLAYER_FACTION_NAME%] has died";

    public DiscordBot(String token, String chatWebhook, long channelId) {
        this.token = token;
        this.chatWebhook = new DiscordWebhook(chatWebhook);
        this.channelId = channelId;
        this.linkRequestMap = new HashMap<>();
    }

    public void initialize() {
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.setActivity(Activity.playing("StarMade"));
        builder.addEventListeners(this);
        try {
            bot = builder.build();
            initializeConfig();
            LogUtils.logMessage(MessageType.INFO, "Successfully initialized bot.");
        } catch(LoginException exception) {
            exception.printStackTrace();
        }

        CommandUpdateAction commands = bot.updateCommands();
        for(CommandInterface commandInterface : StarLoader.getAllCommands()) {
            if(commandInterface instanceof DiscordCommand) commands.addCommands(((DiscordCommand) commandInterface).getCommandData()).queue();
        }
        commands.queue();
    }

    public void initializeConfig() {
        messageConfig = new ConfigFile(DataUtils.getWorldDataPath() + "/message-config.txt");
        serverStartMessage = messageConfig.getConfigurableValue("server-start-message", serverStartMessage);
        serverStopMessage = messageConfig.getConfigurableValue("server-stop-message", serverStopMessage);
        newPlayerMessage = messageConfig.getConfigurableValue("new-player-message", newPlayerMessage);
        playerJoinMessage = messageConfig.getConfigurableValue("player-join-message", playerJoinMessage);
        playerLeaveMessage = messageConfig.getConfigurableValue("player-leave-message", playerLeaveMessage);
        createFactionMessage = messageConfig.getConfigurableValue("create-faction-message", createFactionMessage);
        factionJoinMessage = messageConfig.getConfigurableValue("faction-join-message", factionJoinMessage);
        factionLeaveMessage = messageConfig.getConfigurableValue("faction-leave-message", factionLeaveMessage);
        factionAllyMessage = messageConfig.getConfigurableValue("faction-ally-message", factionAllyMessage);
        factionWarMessage = messageConfig.getConfigurableValue("faction-war-message", factionWarMessage);
        playerKillByPlayerMessage = messageConfig.getConfigurableValue("player-kill-by-player-message", playerKillByPlayerMessage);
        playerKillByEntityMessage = messageConfig.getConfigurableValue("player-kill-by-entity-message", playerKillByEntityMessage);
        playerKillByOtherMessage = messageConfig.getConfigurableValue("player-kill-by-other-message", playerKillByOtherMessage);
        messageConfig.saveConfig();
    }

    public String getBotName() {
        return StarBridge.instance.botName;
    }

    public void handleEvent(Event event) {
        if(event instanceof PlayerCustomCommandEvent) {
            PlayerCustomCommandEvent playerCustomCommandEvent = (PlayerCustomCommandEvent) event;
            //Todo
        } else if(event instanceof PlayerChatEvent) {
            PlayerChatEvent playerChatEvent = (PlayerChatEvent) event;
            if(lastMessage == null || !playerChatEvent.getMessage().text.equals(lastMessage.text)) {
                PlayerData playerData = ServerDatabase.getPlayerData(playerChatEvent.getMessage().sender);
                if(StarLoader.getModFromName("BetterChat") != null) {
                    StringBuilder builder = new StringBuilder();
                    char[] charArray = playerChatEvent.getText().toCharArray();
                    for(int i = 0; i < charArray.length; i ++) {
                        if(charArray[i] == '&') i ++;
                        else builder.append(charArray[i]);
                    }
                    if(playerData != null) sendMessageFromServer(playerData, builder.toString(), playerChatEvent.getMessage());
                    else LogUtils.logMessage(MessageType.ERROR, "Player " + playerChatEvent.getMessage().sender + " doesn't exist in database!");
                } else {
                    if(playerData != null) sendMessageFromServer(playerData, playerChatEvent.getText(), playerChatEvent.getMessage());
                    else LogUtils.logMessage(MessageType.ERROR, "Player " + playerChatEvent.getMessage().sender + " doesn't exist in database!");
                }
                LogUtils.logChat(playerChatEvent.getMessage());
            }
        } else if(event instanceof PlayerJoinWorldEvent) {
            PlayerJoinWorldEvent playerJoinWorldEvent = (PlayerJoinWorldEvent) event;
            if(ServerDatabase.getPlayerData(playerJoinWorldEvent.getPlayerName()) == null) {
                String serverName = (GameCommon.getGameState() != null) ? GameCommon.getGameState().getServerName() : "the server";
                if(serverName.equals("NoName")) serverName = "the server";
                sendBotEventMessage(newPlayerMessage, playerJoinWorldEvent.getPlayerName(), serverName);
                ServerDatabase.addNewPlayerData(playerJoinWorldEvent.getPlayerName());
            } else sendBotEventMessage(playerJoinMessage, playerJoinWorldEvent.getPlayerName());
        } else if(event instanceof PlayerLeaveWorldEvent) {
            sendBotEventMessage(playerLeaveMessage, ((PlayerLeaveWorldEvent) event).getPlayerName());
        } else if(event instanceof FactionCreateEvent) {
            FactionCreateEvent factionCreateEvent = (FactionCreateEvent) event;
            sendBotEventMessage(createFactionMessage, factionCreateEvent.getPlayer().getName(), factionCreateEvent.getFaction().getName());
        } else if(event instanceof PlayerJoinFactionEvent) {
            PlayerJoinFactionEvent playerJoinFactionEvent = (PlayerJoinFactionEvent) event;
            sendBotEventMessage(factionJoinMessage, playerJoinFactionEvent.getPlayer().getName(), playerJoinFactionEvent.getFaction().getName());
        } else if(event instanceof PlayerLeaveFactionEvent) {
            PlayerLeaveFactionEvent playerLeaveFactionEvent = (PlayerLeaveFactionEvent) event;
            sendBotEventMessage(factionLeaveMessage, playerLeaveFactionEvent.getPlayer().getName(), playerLeaveFactionEvent.getFaction().getName());
        } else if(event instanceof PlayerDeathEvent) {
            PlayerDeathEvent playerDeathEvent = (PlayerDeathEvent) event;
            if(playerDeathEvent.getDamager().isSegmentController()) {
                SegmentController segmentController = (SegmentController) playerDeathEvent.getDamager();
                String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
                String entityFactionName = (segmentController.getFactionId() != 0) ? segmentController.getFaction().getName() : "No Faction";
                sendBotEventMessage(playerKillByEntityMessage, playerDeathEvent.getPlayer().getName(), playerFactionName, segmentController.getName(), entityFactionName);
            } else if(playerDeathEvent.getDamager() instanceof PlayerState) {
                PlayerState killerState = (PlayerState) playerDeathEvent.getDamager();
                String killerFactionName = (killerState.getFactionId() != 0) ? killerState.getFactionName() : "No Faction";
                String killedFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
                sendBotEventMessage(playerKillByPlayerMessage, playerDeathEvent.getPlayer().getName(), killedFactionName, killerState.getName(), killerFactionName);
            } else {
                String playerFactionName = (playerDeathEvent.getPlayer().getFactionId() != 0) ? playerDeathEvent.getPlayer().getFactionName() : "No Faction";
                sendBotEventMessage(playerKillByOtherMessage, playerDeathEvent.getPlayer().getName(), playerFactionName);
            }
        } else if(event instanceof FactionRelationChangeEvent) {
            FactionRelationChangeEvent factionRelationChangeEvent = (FactionRelationChangeEvent) event;
            if(factionRelationChangeEvent.getNewRelation().equals(FactionRelation.RType.FRIEND)) {
                sendBotEventMessage(factionAllyMessage, factionRelationChangeEvent.getTo().getName(), factionRelationChangeEvent.getFrom().getName());
            } else if(factionRelationChangeEvent.getNewRelation().equals(FactionRelation.RType.ENEMY)) {
                sendBotEventMessage(factionWarMessage, factionRelationChangeEvent.getTo().getName(), factionRelationChangeEvent.getFrom().getName());
            }
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
                    if(word.charAt(0) == '%' && word.charAt(word.length() - 1) == '%') {
                        builder.append(args[argIndex]);
                        argIndex ++;
                    } else builder.append(word);
                }
                try {
                    sendMessageToDiscord(builder.toString().trim());
                    GameClient.getClientState().getChat().addToVisibleChat("[" + getBotName() + "] " + builder.toString().trim(), "[GENERAL]", false);
                } catch(Exception ignored) { }
            } else LogUtils.logMessage(MessageType.ERROR, "Invalid message arguments count! Should be " + argsCount / 2 + " arguments but only found " + args.length + ".");
        }
    }

    public void sendMessageFromServer(PlayerData playerData, String message, ChatMessage chatMessage) {
        if(playerData.getDiscordId() != -1) {
            try {
                chatWebhook.setAvatarUrl(bot.retrieveUserById(playerData.getDiscordId()).complete(true).getEffectiveAvatarUrl());
            } catch(RateLimitedException e) {
                e.printStackTrace();
            }
        } else resetWebhook();
        chatWebhook.setUsername("[" + playerData.getFactionName() + "] " + playerData.getPlayerName());
        if(message.contains(":")) {
            StringBuilder builder = new StringBuilder();
            StringBuilder emoteBuilder = new StringBuilder();
            char[] charArray = message.toCharArray();
            boolean emoteMode = false;
            for(char c : charArray) {
                if(c == ':') {
                    if(!emoteMode) {
                        builder.append('<');
                        emoteMode = true;
                    } else {
                        builder.append(c);
                        try {
                            Emote emote = bot.getEmotesByName(emoteBuilder.toString(), true).get(0);
                            builder.append(emote.getIdLong());
                        } catch(Exception ignored) { }
                        builder.append('>');
                        emoteMode = false;
                        emoteBuilder = new StringBuilder();
                        continue;
                    }
                } else if(emoteMode) emoteBuilder.append(c);
                builder.append(c);
            }
            chatWebhook.setContent(builder.toString());
        } else chatWebhook.setContent(message);
        try {
            chatWebhook.execute();
        } catch(IOException exception) {
            exception.printStackTrace();
        }
        lastMessage = chatMessage;
        resetWebhook();
    }

    public void sendMessageToServer(String message) {
        GameClient.getClientState().getChat().addToVisibleChat("[" + getBotName() + "] " + message.trim(), "[GENERAL]", false);
    }

    public void sendMessageToDiscord(String message) {
        chatWebhook.setContent(message);
        try {
            chatWebhook.execute();
        } catch(IOException exception) {
            exception.printStackTrace();
        }
        resetWebhook();
    }

    public void resetWebhook() {
        chatWebhook.setUsername(getBotName());
        chatWebhook.setAvatarUrl(StarBridge.instance.botAvatar);
        chatWebhook.setContent("");
    }

    public void addLinkRequest(PlayerState playerState) {
        final PlayerData playerData = ServerDatabase.getPlayerData(playerState.getName());
        if(linkRequestMap.containsKey(playerData)) linkRequestMap.remove(playerData);
        linkRequestMap.put(playerData, (new Random()).nextInt(9999 - 1000) + 1000);
        PlayerUtils.sendMessage(playerState, "Use /link " + linkRequestMap.get(playerData) + " in #" + bot.getTextChannelById(channelId).getName() + " to link your account. This code will expire in 15 minutes.");
        TimerTask task = new TimerTask() {
            public void run() {
                removeLinkRequest(playerData);
            }
        };
        Timer timer = new Timer("link-timer_" + linkRequestMap.get(playerData));
        timer.schedule(task, 900000);
    }

    public void removeLinkRequest(PlayerData playerData) {
        linkRequestMap.remove(playerData);
    }

    public PlayerData getLinkRequest(int linkId) {
        for(Map.Entry<PlayerData, Integer> entry : linkRequestMap.entrySet()) {
            if(entry.getValue() == linkId) return entry.getKey();
        }
        return null;
    }

    public void sendServerStartMessage() {
        sendMessageToDiscord(serverStartMessage);
    }

    public void sendServerStopMessage() {
        sendMessageToDiscord(serverStopMessage);
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        sendServerStartMessage();
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        sendServerStopMessage();
    }

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if(event.getGuild() != null) {
            CommandInterface commandInterface = StarLoader.getCommand(event.getName());
            if(commandInterface != null) {
                if(commandInterface instanceof DiscordCommand) ((DiscordCommand) commandInterface).execute(event);
                else event.reply("This command is only available in-game").queue();
            } else event.reply("/" + event.getCommandPath().replace("/", " ") + " is not a valid command").queue();
            event.getHook().deleteOriginal().queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String content = event.getMessage().getContentDisplay();
        if(content.length() > 0) {
            if(!event.getAuthor().isBot() && !event.isWebhookMessage()) {
                if(event.getChannel().getIdLong() == channelId) {
                    if(content.charAt(0) != '/') {
                        GameClient.getClientState().getChat().addToVisibleChat("[" + event.getAuthor().getName() + "] " + event.getMessage().getContentDisplay(), "[GENERAL]", false);
                    } else event.getMessage().delete().queue();
                }
            }
        }
    }

    public boolean hasRole(Member member, long roleId) {
        for(Role role : member.getRoles()) if(role.getIdLong() == roleId) return true;
        return false;
    }
}
