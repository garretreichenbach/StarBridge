package thederpgamer.starbridge;

import api.ModPlayground;
import api.common.GameCommon;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import api.utils.other.HashList;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.LogManager;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.server.bot.BotThread;
import thederpgamer.starbridge.server.bot.DiscordBot;
import thederpgamer.starbridge.server.commands.*;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main mod class for StarBridge.
 *
 * @version 1.0 - [03/10/2021]
 * @author TheDerpGamer
 */
public class StarBridge extends StarMod {

    //Instance
    public static void main(String[] args) { }
    private static StarBridge instance;
    public static StarBridge getInstance() {
        return instance;
    }
    public StarBridge() {
        instance = this;
    }

    //Constants
    public static final long AUTO_RESTART_MS = 21600000L; //6 hours between restarts
    public static final long PLAY_TIME_UPDATE = 300000L; //5 minutes

    //Other
    public BotThread botThread;

    @Override
    public void onEnable() {
        instance = this;

        ConfigManager.initialize();
        LogManager.initialize();
        doOverwrites();
        registerListeners();
        registerCommands();
        startRunners();

        (botThread = new BotThread(ConfigManager.getMainConfig())).start();
    }

    @Override
    public void onDisable() {
        if(botThread != null && botThread.getBot() != null) botThread.getBot().sendServerStopMessage();
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerCustomCommandEvent.class, new Listener<PlayerCustomCommandEvent>() {
            @Override
            public void onEvent(PlayerCustomCommandEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(PlayerChatEvent.class, new Listener<PlayerChatEvent>() {
            @Override
            public void onEvent(PlayerChatEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(PlayerJoinWorldEvent.class, new Listener<PlayerJoinWorldEvent>() {
            @Override
            public void onEvent(PlayerJoinWorldEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveWorldEvent.class, new Listener<PlayerLeaveWorldEvent>() {
            @Override
            public void onEvent(PlayerLeaveWorldEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(FactionCreateEvent.class, new Listener<FactionCreateEvent>() {
            @Override
            public void onEvent(FactionCreateEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                getBot().handleEvent(event);
            }
        }, this);

        StarLoader.registerListener(FactionRelationChangeEvent.class, new Listener<FactionRelationChangeEvent>() {
            @Override
            public void onEvent(FactionRelationChangeEvent event) {
                getBot().handleEvent(event);
            }
        }, this);
    }

    private void registerCommands() {
        CommandInterface[] commandArray = new CommandInterface[] {
                new ListCommand(),
                new LinkCommand(),
                new ClearDataCommand(),
                new RestartCommand(),
                new InfoPlayerCommand(),
                new InfoFactionCommand()
        };
        for(CommandInterface commandInterface : commandArray) {
            StarLoader.registerCommand(commandInterface);
        }
    }

    private void doOverwrites() {
        try {
            Field commandsField = StarLoader.class.getDeclaredField("commands");
            commandsField.setAccessible(true);
            HashList<StarMod, CommandInterface> commands = (HashList<StarMod, CommandInterface>) commandsField.get(null);
            commands.getList(ModPlayground.inst).remove(StarLoader.getCommand("help"));
            commands.getList(ModPlayground.inst).add(new HelpDiscordCommand());
            commandsField.set(null, commands);
        } catch(NoSuchFieldException | IllegalAccessException | ClassCastException exception) {
            exception.printStackTrace();
        }
    }

    private void startRunners() {
       if(GameCommon.isDedicatedServer()) {
           { //Play Timer
               Timer timer = new Timer();
               timer.schedule(new TimerTask() {
                   @Override
                   public void run() {
                       for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                           Objects.requireNonNull(ServerDatabase.getPlayerDataWithoutNull(playerState.getName())).updatePlayTime(PLAY_TIME_UPDATE);
                       }
                   }
               }, PLAY_TIME_UPDATE);
           }

           { //Auto Restart
               int defaultShutdownTimer = ConfigManager.getMainConfig().getInt("default-shutdown-timer");
               long autoRestart = AUTO_RESTART_MS - (defaultShutdownTimer * 1000L);
               Timer timer = new Timer();

               timer.scheduleAtFixedRate(new TimerTask() {
                   @Override
                   public void run() {
                       StarBridge.getInstance().getBot().sendMessageToServer(StarBridge.getInstance().getBot().getBotName(), "Server Restarting in " + defaultShutdownTimer + " seconds.");
                       StarBridge.getInstance().getBot().sendMessageToDiscord(":octagonal_sign: Server Restarting in " + defaultShutdownTimer + " seconds.");
                       GameServer.getServerState().addCountdownMessage(defaultShutdownTimer, "Server restarting in " + defaultShutdownTimer + " seconds.\n");
                   }
               }, autoRestart, AUTO_RESTART_MS);
           }
       }
    }

    public DiscordBot getBot() {
        return botThread.getBot();
    }

    //API Methods

    /**
     * Fetches a player's Avatar URL based off their name. Returns the default avatar if they haven't linked their discord account or if the player doesn't exist.
     * @param playerName The player's name
     * @return The player's Avatar URL
     */
    public String getAvatarURL(String playerName) {
        long discordId = ServerDatabase.getPlayerDataWithoutNull(playerName).getDiscordId();
        if(discordId != -1) {
            try {
                return getBot().bot.retrieveUserById(discordId).complete(true).getEffectiveAvatarUrl();
            } catch(RateLimitedException exception) {
                exception.printStackTrace();
            }
        }
        return "https://" + ConfigManager.getMainConfig().getString("bot-avatar");
    }

    /**
     * Fetches the URL of an emote by it's name. Returns null if no emote is found with a matching name.
     * @param emoteName The name of the emote
     * @return The URL of the emote
     */
    public String getEmoteURL(String emoteName) {
        return getBot().bot.getEmotesByName(emoteName, true).get(0).getImageUrl();
    }

    /**
     * Fetches a player's user ID based off their name. Returns -1 if the player hasn't linked their discord account.
     * @param playerName The player's name
     * @return The player's user ID
     */
    public long getUserId(String playerName) {
        return ServerDatabase.getPlayerDataWithoutNull(playerName).getDiscordId();
    }

    /**
     * Sends a message to the in-game server.
     * @param message The message to send
     */
    public void sendMessageToServer(String message) {
        getBot().sendMessageToServer(getBot().getBotName(), message);
    }

    /**
     * Sends a message to the discord channel.
     * @param message The message to send
     */
    public void sendMessageToDiscord(String message) {
        getBot().sendMessageToDiscord(message);
    }
}
