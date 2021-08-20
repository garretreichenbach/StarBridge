package thederpgamer.starbridge.server.bot;

import api.listener.events.Event;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
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
import org.schema.game.common.data.chat.ChannelRouter;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.network.objects.ChatMessage;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.RegisteredClientOnServer;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.config.ConfigFile;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.DiscordWebhook;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.server.commands.*;
import thederpgamer.starbridge.utils.DataUtils;
import thederpgamer.starbridge.utils.LogUtils;
import thederpgamer.starbridge.utils.MessageType;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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

    //Message Config
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
    private String playerKillByPlayerMessage = ":skull_crossbones: %PLAYER_NAME_1% %PLAYER_1_FACTION_NAME% was slain by %PLAYER_NAME_2% %PLAYER_2_FACTION_NAME%";
    private String playerKillByEntityMessage = ":skull_crossbones: %PLAYER_NAME% %PLAYER_FACTION_NAME% was slain by %ENTITY_NAME% %ENTITY_FACTION_NAME%";
    private String playerKillByOtherMessage = ":skull_crossbones: %PLAYER_NAME% %PLAYER_FACTION_NAME% has died";

    //private final String firstDMMessage = "You received this message from in-game but were not online to see it, so it has been delivered to you here. If you want to change this setting and more use /psettings in this DM or in the server chat channel. This is a one time message, so use /commands to see more options.";

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
            LogUtils.logException("An exception occurred while initializing the bot", exception);
        }
        registerCommands();
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

    private void registerCommands() {
        CommandInterface[] commandArray = new CommandInterface[] {
                new ListCommand(),
                new LinkCommand(),
                new ClearCommand(),
                new RestartCommand(),
                new InfoPlayerCommand(),
                new InfoFactionCommand(),
                new HelpDiscordCommand()
        };
        CommandUpdateAction commands = bot.updateCommands();

        for(CommandInterface commandInterface : commandArray) {
            commands.addCommands(((DiscordCommand) commandInterface).getCommandData()).queue();
            LogUtils.logMessage(MessageType.INFO, "Registered command /" +  commandInterface.getCommand());
        }
        commands.queue();
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
                ChatMessage chatMessage = playerChatEvent.getMessage();
                PlayerData playerData = ServerDatabase.getPlayerData(chatMessage.sender);
                /*
                if(StarLoader.getModFromName("BetterChat") != null) {
                    StringBuilder builder = new StringBuilder();
                    char[] charArray = playerChatEvent.getText().toCharArray();
                    for(int i = 0; i < charArray.length; i ++) {
                        if(charArray[i] == '&') i ++;
                        else builder.append(charArray[i]);
                    }
                    sendMessageFromServer(playerData, builder.toString(), playerChatEvent.getMessage());
                } else {
                    sendMessageFromServer(playerData, playerChatEvent.getText(), playerChatEvent.getMessage());
                }
                 */

                switch(playerChatEvent.getMessage().receiverType) {
                    case DIRECT:
                        /* You can't send pms to offline players, so this functionality is useless right now
                        PlayerData receiverData = ServerDatabase.getPlayerData(chatMessage.receiver);
                        User receiver = bot.retrieveUserById(receiverData.getDiscordId()).complete();
                        if(receiver != null) {
                            StringBuilder dmBuilder = new StringBuilder();
                            if(receiverData.isFirstDM()) {
                                dmBuilder.append(firstDMMessage).append('\n');
                                receiverData.setFirstDM(false);
                            }
                            dmBuilder.append(chatMessage.sender).append(": ");
                            dmBuilder.append(chatMessage.text);
                            sendPrivateMessage(playerData, receiverData, dmBuilder.toString());
                            //message = (new MessageBuilder(dmBuilder.toString())).build();
                            //channel = receiver.openPrivateChannel().complete();
                        }
                         */
                        LogUtils.logChat(chatMessage, "PM WITH " + chatMessage.receiver);
                        break;
                    case SYSTEM: //Not sure what this is
                        break;
                    case CHANNEL:
                        if((chatMessage.getChannel() == null || (chatMessage.receiver.equals("all")) && chatMessage.getChannel().getType().equals(ChannelRouter.ChannelType.ALL))) {
                            sendMessageFromServer(playerData, chatMessage.text, chatMessage);
                            LogUtils.logChat(chatMessage, "GENERAL");
                        } else if(chatMessage.getChannel() != null && chatMessage.getChannel().getType().equals(ChannelRouter.ChannelType.PUBLIC)) {
                            sendMessageFromServer(playerData, chatMessage.text, chatMessage);
                            LogUtils.logChat(chatMessage, chatMessage.getChannel().getName().toUpperCase());
                        }
                        break;
                }
            }
        } else if(event instanceof PlayerJoinWorldEvent) {
            PlayerJoinWorldEvent playerJoinWorldEvent = (PlayerJoinWorldEvent) event;
            ServerDatabase.getPlayerData(playerJoinWorldEvent.getPlayerName());
            sendBotEventMessage(playerJoinMessage, playerJoinWorldEvent.getPlayerName());
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
                    if(word.charAt(1) == '%' && word.charAt(word.length() - 2) == '%') {
                        builder.append('[').append(args[argIndex]).append(']');
                        argIndex ++;
                    } else if(word.charAt(0) == '%' && word.charAt(word.length() - 1) == '%') {
                        builder.append(args[argIndex]);
                        argIndex ++;
                    } else builder.append(word);
                }
                try {
                    sendMessageToDiscord(builder.toString().trim());
                    sendMessageToServer(getBotName(), builder.toString().trim());
                    //GameServer.getServerState().chat(GameServer.getServerState().getChat(), builder.toString().trim(), "[" + getBotName() + "]", false);
                } catch(Exception exception) {
                    LogUtils.logException("An exception encountered while trying to send a message from the bot", exception);
                    //LogUtils.logMessage(MessageType.ERROR, "Exception encountered while trying to send message from bot:\n" + exception.getMessage());
                }
            } else LogUtils.logMessage(MessageType.ERROR, "Invalid message arguments count! Should be " + argsCount / 2 + " arguments but only found " + args.length + ".");
        }
    }

    public void setBotAvatar(String link) throws IOException {
        Icon.IconType type = getImageType(link);
        URL url = new URL(link);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("User-Agent", "NING/1.0");
        InputStream stream = urlConnection.getInputStream();
        bot.getSelfUser().getManager().setAvatar(Icon.from(stream, type)).queue();
    }

    private Icon.IconType getImageType(String link) {
        if(link.toLowerCase().endsWith(".png")) return Icon.IconType.PNG;
        else if(link.toLowerCase().endsWith(".jpeg") || link.toLowerCase().endsWith(".jpg")) return Icon.IconType.JPEG;
        else if(link.toLowerCase().endsWith(".webp")) return Icon.IconType.WEBP;
        else if(link.toLowerCase().endsWith(".gif")) return Icon.IconType.GIF;
        else return Icon.IconType.UNKNOWN;
    }

    public void sendPrivateMessage(PlayerData sender, PlayerData receiver, String message) {
        if(sender.getDiscordId() != -1) {
            try {
                setBotAvatar(bot.retrieveUserById(sender.getDiscordId()).complete(true).getEffectiveAvatarUrl());
            } catch(RateLimitedException | IOException exception) {
                LogUtils.logException("An exception occurred while trying to send a message from the server", exception);
            }
        } else resetWebhook();
        if(sender.inFaction()) bot.getSelfUser().getManager().setName(sender.getPlayerName() + "[" + sender.getFactionName() + "]").queue();
        else bot.getSelfUser().getManager().setName(sender.getPlayerName()).queue();
        User receiverUser = bot.retrieveUserById(receiver.getDiscordId()).complete();
        Message m;
        if(receiverUser != null) {
            PrivateChannel channel = receiverUser.openPrivateChannel().complete();
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
                                if(emote.getGuild() != null) {
                                    builder.append(emote.getIdLong());
                                    builder.append('>');
                                } else builder.deleteCharAt(builder.lastIndexOf("<"));
                            } catch(Exception exception) {
                                LogUtils.logException("An exception occurred while trying to fetch emote \"" + emoteBuilder.substring(emoteBuilder.toString().indexOf(":"), emoteBuilder.toString().lastIndexOf(":")).trim() + "\"", exception);
                            }
                            emoteMode = false;
                            emoteBuilder = new StringBuilder();
                            continue;
                        }
                    } else if(emoteMode) emoteBuilder.append(c);
                    builder.append(c);
                }
                m = (new MessageBuilder(builder.toString())).build();
            } else  m = (new MessageBuilder(message).build());
            channel.sendMessage(m).queue();
        }
    }

    public void sendMessageFromServer(PlayerData playerData, String message, ChatMessage chatMessage) {
        if(playerData.getDiscordId() != -1) {
            try {
                chatWebhook.setAvatarUrl(bot.retrieveUserById(playerData.getDiscordId()).complete(true).getEffectiveAvatarUrl());
            } catch(RateLimitedException exception) {
                LogUtils.logException("An exception occurred while trying to send a message from the server", exception);
            }
        } else resetWebhook();
        if(playerData.inFaction()) chatWebhook.setUsername(playerData.getPlayerName() + "[" + playerData.getFactionName() + "]");
        else chatWebhook.setUsername(playerData.getPlayerName());
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
                            if(emote.getGuild() != null) {
                                builder.append(emote.getIdLong());
                                builder.append('>');
                            } else builder.deleteCharAt(builder.lastIndexOf("<"));
                        } catch(Exception exception) {
                            LogUtils.logException("An exception occurred while trying to fetch emote \"" + emoteBuilder.substring(emoteBuilder.toString().indexOf(":"), emoteBuilder.toString().lastIndexOf(":")).trim() + "\"", exception);
                        }
                        emoteMode = false;
                        emoteBuilder = new StringBuilder();
                        continue;
                    }
                } else if(emoteMode) emoteBuilder.append(c);
                builder.append(c);
            }
            chatWebhook.setContent(builder.toString().trim());
        } else chatWebhook.setContent(message);
        try {
            chatWebhook.execute();
        } catch(IOException exception) {
            LogUtils.logException("An exception occurred while trying to send a message from the server", exception);
        }
        lastMessage = chatMessage;
        resetWebhook();
    }

    public void sendMessageToServer(String sender, String message) {
        for(RegisteredClientOnServer client : GameServerState.instance.getClients().values()) {
            try {
                client.serverMessage("[" + sender + "] " + message);
            } catch(IOException exception) {
                LogUtils.logException("An exception occurred while trying to send a message to the server", exception);
            }
        }
    }

    public void sendMessageToDiscord(String message) {
        chatWebhook.setContent(message);
        try {
            chatWebhook.execute();
        } catch(IOException exception) {
            LogUtils.logException("An exception occurred while trying to send a message from the server", exception);
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
                if(commandInterface instanceof DiscordCommand) {
                    if(commandInterface.isAdminOnly() && !hasRole(Objects.requireNonNull(event.getMember()), StarBridge.instance.adminRoleId)) {
                        event.reply("You don't have permission to use this command!").queue();
                        return;
                    }
                    ((DiscordCommand) commandInterface).execute(event);
                    if(!event.isAcknowledged()) {
                        try {
                            event.acknowledge(true).queue();
                        } catch(Exception exception) {
                            LogUtils.logException("An exception occurred while trying to acknowledge command \"/" + commandInterface.getCommand() + "\"", exception);
                        }
                    }
                } else event.reply("This command is only available in-game").queue();
            } else event.reply("/" + event.getCommandPath().replace("/", " ") + " is not a valid command").queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String content = event.getMessage().getContentDisplay().trim();
        if(content.length() > 0) {
            if(!event.getAuthor().isBot() && !event.isWebhookMessage()) {
                if(event.getChannel().getIdLong() == channelId) {
                    if(content.charAt(0) != '/') {
                        //GameServer.getServerState().chat(GameServer.getServerState().getChat(), content, "[" + event.getAuthor().getName() + "]", false);
                        sendMessageToServer(event.getAuthor().getName(), content.trim());
                        logChat(event.getAuthor().getName(), content.trim());
                    } else event.getMessage().delete().queue();
                }
            }
        }
    }

    private void logChat(String sender, String message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.sender = sender;
        chatMessage.text = message;
        LogUtils.logChat(chatMessage, "GENERAL");
    }

    public boolean hasRole(Member member, long roleId) {
        for(Role role : member.getRoles()) if(role.getIdLong() == roleId) return true;
        return false;
    }
}
