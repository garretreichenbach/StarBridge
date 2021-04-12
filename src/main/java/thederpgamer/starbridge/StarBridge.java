package thederpgamer.starbridge;

import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.player.*;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.mod.config.PersistentObjectUtil;
import api.utils.StarRunnable;
import thederpgamer.starbridge.server.bot.BotThread;
import thederpgamer.starbridge.server.bot.DiscordBot;
import thederpgamer.starbridge.server.bot.commands.LinkCommand;
import thederpgamer.starbridge.server.bot.commands.ListCommand;
import thederpgamer.starbridge.utils.LogUtils;

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
            "chat-channel-id: CHANNEL ID",
            "command-channel-id: CHANNEL ID"
    };
    public boolean debugMode = false;
    public long autoSaveFrequency = 10000;
    public int maxWorldLogs = 5;
    public String botName;
    public String botToken;
    public String botAvatar;
    public long serverId;
    public String chatWebhook;
    public long chatChannelId;
    public long commandChannelId;

    //Data
    public BotThread botThread;

    @Override
    public void onEnable() {
        instance = this;
        initConfig();
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
        chatChannelId = config.getLong("chat-channel-id");
        commandChannelId = config.getLong("command-channel-id");
    }

    private void initialize() {
        LogUtils.initialize();
        (botThread = new BotThread(botToken, chatWebhook, chatChannelId, commandChannelId)).start();
    }

    private void registerCommands() {
        StarLoader.registerCommand(new ListCommand());
        StarLoader.registerCommand(new LinkCommand());
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
}
