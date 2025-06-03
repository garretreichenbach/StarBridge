package thederpgamer.starbridge.data.permissions;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public interface IPermissibleAction {

	/**
	 * Gets the group that is required to perform this action, or null if no group is required.
	 * @return The required permission group for this action, or null if no group is required.
	 */
	PermissionGroup getRequiredGroup();
}
