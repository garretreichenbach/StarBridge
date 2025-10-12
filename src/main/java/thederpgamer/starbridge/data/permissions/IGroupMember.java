package thederpgamer.starbridge.data.permissions;

import thederpgamer.starbridge.data.other.Pair;

import java.util.Set;

/**
 * Represents a member of a group that can hold permissions.
 * This interface extends IPermissionHolder to include group-specific functionality.
 * It allows checking group membership, retrieving the group identifier, and managing group permissions.
 *
 * @author TheDerpGamer
 */
public interface IGroupMember extends IPermissionHolder {

	/**
	 * Checks if this member is in any group.
	 * @return True if the member is in a group, false otherwise.
	 */
	boolean inAnyGroup();

	/**
	 * Checks if this member is in a specific group.
	 * @param groupUid The unique identifier of the group to check.
	 * @return True if the member is in the specified group, false otherwise.
	 */
	boolean inGroup(String groupUid);

	/**
	 * Gets the unique identifier of the group this member belongs to.
	 * If the member is not in any group, it returns "NONE".
	 * @return The unique identifier of the group, or "NONE" if not in a group.
	 */
	String getGroup();

	/**
	 * Sets the group for this member.
	 * If the groupUid is null or empty, it will set the group to "NONE".
	 * @param groupUid The unique identifier of the group.
	 */
	void setGroup(String groupUid);

	/**
	 * Gets the permissions that this group member has from their group.
	 * Does not include permissions granted directly to the member.
	 * @return Set of permissions from the group, where each permission is represented as a Pair of node and value.
	 */
	Set<Pair<String, Object>> getPermissionsFromGroup();

	/**
	 * Gets all permissions for this group member.
	 * @param groupUid The unique identifier of the group to check permissions against.
	 * @return Set of all permissions, combining group permissions and member-specific permissions.
	 */
	default Set<Pair<String, Object>> getAllPermissions(String groupUid) {
		if(inGroup(groupUid)) {
			Set<Pair<String, Object>> groupPermissions = getPermissionsFromGroup();
			Set<Pair<String, Object>> memberPermissions = getPermissions();
			groupPermissions.addAll(memberPermissions);
			return groupPermissions;
		}
		return getPermissions();
	}
}
