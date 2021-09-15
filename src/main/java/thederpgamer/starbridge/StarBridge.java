package thederpgamer.starbridge;

import api.ModPlayground;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.utils.StarRunnable;
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

        botThread = new BotThread(ConfigManager.getMainConfig());
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
        //Play Timer
        new StarRunnable() {
            @Override
            public void run() {
                for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                    Objects.requireNonNull(ServerDatabase.getPlayerData(playerState.getName())).updatePlayTime(10000 / 2);
                }
            }
        }.runTimer(this, 10000 / 2);
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
        long discordId = ServerDatabase.getPlayerData(playerName).getDiscordId();
        if(discordId != -1) {
            try {
                return getBot().bot.retrieveUserById(discordId).complete(true).getEffectiveAvatarUrl();
            } catch(RateLimitedException exception) {
                exception.printStackTrace();
            }
        }
        return ConfigManager.getMainConfig().getString("bot-avatar");
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
        return ServerDatabase.getPlayerData(playerName).getDiscordId();
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
