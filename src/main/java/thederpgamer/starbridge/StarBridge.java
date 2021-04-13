package thederpgamer.starbridge;

import api.ModPlayground;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.mod.config.PersistentObjectUtil;
import api.utils.StarRunnable;
import api.utils.game.chat.CommandInterface;
import api.utils.other.HashList;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.server.bot.BotThread;
import thederpgamer.starbridge.server.bot.DiscordBot;
import thederpgamer.starbridge.server.commands.ClearCommand;
import thederpgamer.starbridge.server.commands.HelpDiscordCommand;
import thederpgamer.starbridge.server.commands.LinkCommand;
import thederpgamer.starbridge.server.commands.ListCommand;
import thederpgamer.starbridge.utils.LogUtils;
import java.lang.reflect.Field;

/**
 * StarBridge.java
 * <Description>
 *
 * @since 03/10/2021
 * @author TheDerpGamer
 */
public class StarBridge extends StarMod {

    //Instance
    public StarBridge() {
        instance = this;
    }
    public static StarBridge instance;
    public static void main(String[] args) { }

    //Config
    private final String[] defaultConfig = {
            "debug-mode: false",
            "auto-save-frequency: 10000",
            "max-world-logs: 5",
            "bot-name: BOT NAME",
            "bot-token: BOT TOKEN",
            "bot-avatar: BOT AVATAR",
            "server-id: SERVER ID",
            "chat-webhook: WEBHOOK",
            "channel-id: CHANNEL ID",
            "admin-role-id: ADMIN ROLE ID"
    };
    public boolean debugMode = false;
    public long autoSaveFrequency = 10000;
    public int maxWorldLogs = 5;
    public String botName;
    public String botToken;
    public String botAvatar;
    public long serverId;
    public String chatWebhook;
    public long channelId;
    public long adminRoleId;

    //Data
    public BotThread botThread;

    @Override
    public void onEnable() {
        instance = this;
        initConfig();
        doOverwrites();
        initialize();
        registerCommands();
        registerListeners();
        startRunners();
    }

    private void initConfig() {
        FileConfiguration config = getConfig("config");
        config.saveDefault(defaultConfig);

        debugMode = config.getConfigurableBoolean("debug-mode", false);
        autoSaveFrequency = config.getConfigurableLong("auto-save-frequency", 10000);
        maxWorldLogs = config.getConfigurableInt("max-world-logs", 5);
        botName = config.getString("bot-name");
        botToken = config.getString("bot-token");
        botAvatar = "https://" + config.getString("bot-avatar");
        serverId = config.getLong("server-id");
        chatWebhook = "https://" + config.getString("chat-webhook");
        channelId = config.getLong("channel-id");
        adminRoleId = config.getLong("admin-role-id");
    }

    private void initialize() {
        LogUtils.initialize();
        (botThread = new BotThread(botToken, chatWebhook, channelId)).start();
    }

    private void registerCommands() {
        StarLoader.registerCommand(new ListCommand());
        StarLoader.registerCommand(new LinkCommand());
        StarLoader.registerCommand(new ClearCommand());
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
        //Auto Saver
        new StarRunnable() {
            @Override
            public void run() {
                PersistentObjectUtil.save(getSkeleton());
                getBot().messageConfig.saveConfig();
            }
        }.runTimer(this, autoSaveFrequency);
    }

    public DiscordBot getBot() {
        return botThread.getBot();
    }

    public String getAvatarURL(String playerName) {
        if(ServerDatabase.getPlayerData(playerName) != null) {
            long discordId = ServerDatabase.getPlayerData(playerName).getDiscordId();
            if(discordId != -1) {
                try {
                    return getBot().bot.retrieveUserById(discordId).complete(true).getEffectiveAvatarUrl();
                } catch(RateLimitedException e) {
                    e.printStackTrace();
                }
            }
        }
        return botAvatar;
    }

    public String getEmoteURL(String emoteName) {
        return getBot().bot.getEmotesByName(emoteName, true).get(0).getImageUrl();
    }

    public long getUserId(String playerName) {
        if(ServerDatabase.getPlayerData(playerName) != null) {
            return ServerDatabase.getPlayerData(playerName).getDiscordId();
        }
        return -1;
    }
}
