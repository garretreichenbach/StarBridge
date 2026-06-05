package videogoose.starbridge.data.permissions;

import videogoose.starbridge.data.other.Pair;

/**
 * Defines actions that can only be performed if the user has the required permission node.
 * Each action specifies a permission node and associated value via {@link #getRequiredPermission()},
 * and can optionally require admin-only access via {@link #isAdminOnly()}.
 *
 * @author VideoGoose
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
