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

	public static final String[] defaultMainConfig = {
			"debug-mode: false",
			"bot-name: BOT_NAME",
			"bot-token: BOT_TOKEN",
			"bot-avatar: BOT_AVATAR",
			"server-id: SERVER_ID",
			"chat-channel-id: CHAT_CHANNEL_ID",
			"log-channel-id: LOG_CHANNEL_ID",
			"restart-timer: 3600000", //6 hours
			"default-shutdown-timer: 900000" //15 minutes
	};
	//Main Config
	private static FileConfiguration mainConfig;
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