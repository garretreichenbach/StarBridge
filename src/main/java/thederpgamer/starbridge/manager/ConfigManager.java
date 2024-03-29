package thederpgamer.starbridge.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.starbridge.StarBridge;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [08/23/2021]
 */
public class ConfigManager {

    //Main Config
    private static FileConfiguration mainConfig;
    public static final String[] defaultMainConfig = {
            "debug-mode: false",
            "max-world-logs: 5",
            "restart-timer: ", //6 hours
            "default-shutdown-timer: 900000", //15 minutes
            "bot-name: BOT_NAME",
            "bot-token: BOT_TOKEN",
            "bot-avatar: BOT_AVATAR",
            "server-id: SERVER_ID",
            "chat-webhook: CHAT_WEBHOOK",
            "chat-channel-id: CHAT_CHANNEL_ID",
            "log-webhook: LOG_WEBHOOK",
            "log-channel-id: LOG_CHANNEL_ID",
            "admin-role-id: ADMIN_ROLE_ID",
            "kick-non-admin-alts: true",
            "tier-1-role-id: 1055652219497758725",
            "tier-2-role-id: 1055656604256706564"
    };

    // System names
    private static FileConfiguration systemNamesConfig;

    public static void initialize() {
        mainConfig = StarBridge.getInstance().getConfig("config");
        mainConfig.saveDefault(defaultMainConfig);
        systemNamesConfig = StarBridge.getInstance().getConfig("systems");
    }

    public static FileConfiguration getMainConfig() {
        return mainConfig;
    }

    public static FileConfiguration getSystemNamesConfig() {
        return systemNamesConfig;
    }
}