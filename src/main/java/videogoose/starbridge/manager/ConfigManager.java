package videogoose.starbridge.manager;

import api.mod.config.FileConfiguration;
import videogoose.starbridge.StarBridge;

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
			"default-shutdown-timer: 900000", //15 minutes
			"admin-role-id: ADMIN_ROLE_ID"
	};
	public static final String[] defaultErrorsConfig = {
			"enabled: true",
			"min-post-interval-ms: 300000", //5 minutes between repeat alerts for the same error
			"fingerprint-frames: 6" //top stack frames used to identify a recurring error
	};
	//Main Config
	private static FileConfiguration mainConfig;
	// System names
	private static FileConfiguration systemNamesConfig;
	// Error-management rules (muted fingerprints/patterns/thresholds)
	private static FileConfiguration errorsConfig;

	public static void initialize() {
		mainConfig = StarBridge.getInstance().getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
		systemNamesConfig = StarBridge.getInstance().getConfig("systems");
		errorsConfig = StarBridge.getInstance().getConfig("errors");
		errorsConfig.saveDefault(defaultErrorsConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}

	public static FileConfiguration getSystemNamesConfig() {
		return systemNamesConfig;
	}

	public static FileConfiguration getErrorsConfig() {
		return errorsConfig;
	}
}