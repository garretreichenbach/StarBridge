package thederpgamer.starbridge.data.permissions;

import thederpgamer.starbridge.data.other.Pair;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public interface IPermissibleAction {

	/**
	 * Gets the permission node required to perform this action, or null if no permission is required.
	 * @return A Pair containing the permission node as a String and the value associated with it, or null if no permission is required.
	 */
	Pair<String, Object> getRequiredPermission();

	default boolean isAdminOnly() {
		return false;
	}
}
