package thederpgamer.starbridge.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.manager.ConfigManager;

import java.util.List;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class DiscordUtils {

	private static JDA getJDA() {
		return StarBridge.getBot().getJDA();
	}

	public static Role getRoleByName(String name) {
		List<Role> roles = getJDA().getRolesByName(name, true);
		if(!roles.isEmpty()) return roles.get(0);
		else return null;
	}

	public static Role getRoleById(String id) {
		return getJDA().getRoleById(id);
	}

	public static Role getStaffRole() {
		long roleId = ConfigManager.getMainConfig().getLong("admin-role-id");
		return getRoleById(String.valueOf(roleId));
	}
}
